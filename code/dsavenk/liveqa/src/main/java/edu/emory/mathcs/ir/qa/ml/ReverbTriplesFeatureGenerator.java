package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dsavenk on 8/21/15.
 */
public class ReverbTriplesFeatureGenerator implements FeatureGeneration {
    public static final String PREDICATE_FIELDNAME = "predicate";
    public static final String OBJECT_FIELDNAME = "object";
    public static final String SUBJECT_FIELDNAME = "subject";
    private static final String RELATIONS_COUNT_FEATURENAME =
            "reverb_relations";
    private static final String NORELATIONS_FEATURENAME = "no_reverb_relations";

    private final IndexSearcher searcher_;
    private final QueryBuilder queryBuilder_ =
            new QueryBuilder(new EnglishAnalyzer());

    public ReverbTriplesFeatureGenerator(String reverbIndexLocation)
            throws IOException {
        Directory directory = FSDirectory.open(
                FileSystems.getDefault().getPath(reverbIndexLocation));
        final IndexReader indexReader = DirectoryReader.open(directory);
        searcher_ = new IndexSearcher(indexReader);
    }

    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        Map<String, Double> features = new HashMap<>();
        int countRelations = 0;

        final String[] titleChunks = question.getTitle().getChunks();
        final String[] bodyChunks = question.getBody().getChunks();
        final String[] answerChunks = answer.getAnswer().getChunks();

        for (String answerChunk : answerChunks) {
            Query subjAnswerQuery = queryBuilder_.createPhraseQuery(
                    SUBJECT_FIELDNAME, answerChunk);
            Query objAnswerQuery = queryBuilder_.createPhraseQuery(
                    OBJECT_FIELDNAME, answerChunk);
            if (subjAnswerQuery == null || objAnswerQuery == null) continue;

            for (String titleChunk : titleChunks) {
                Query subjTitleQuery = queryBuilder_.createPhraseQuery(
                        SUBJECT_FIELDNAME, titleChunk);
                Query objTitleQuery = queryBuilder_.createPhraseQuery(
                        OBJECT_FIELDNAME, titleChunk);
                if (subjTitleQuery == null || objTitleQuery == null) continue;

                // Skip same queries.
                if (subjTitleQuery.toString().equals(
                        subjAnswerQuery.toString())) continue;

                BooleanQuery q1 = new BooleanQuery();
                q1.add(subjTitleQuery, BooleanClause.Occur.MUST);
                q1.add(objAnswerQuery, BooleanClause.Occur.MUST);
                BooleanQuery q2 = new BooleanQuery();
                q1.add(objTitleQuery, BooleanClause.Occur.MUST);
                q1.add(subjAnswerQuery, BooleanClause.Occur.MUST);
                try {
                    TopDocs docs1 = searcher_.search(q1, 1);
                    TopDocs docs2 = searcher_.search(q2, 1);
                    if (docs1.totalHits > 0 || docs2.totalHits > 0) {
                        ++countRelations;
                        int docid = docs1.scoreDocs.length > 0 ? docs1.scoreDocs[0].doc : docs2.scoreDocs[0].doc;
                        System.err.println(
                                titleChunk + "\t-\t" + answerChunk + "\t-\t" +
                                        searcher_.doc(docid).get(SUBJECT_FIELDNAME) +
                                        "\t-\t" + searcher_.doc(docid).get(PREDICATE_FIELDNAME) +
                                        "\t-\t" + searcher_.doc(docid).get(OBJECT_FIELDNAME));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        if (countRelations > 0) {
            features.put(RELATIONS_COUNT_FEATURENAME, (double) countRelations);
        } else {
            features.put(NORELATIONS_FEATURENAME, 1.0);
        }
        return features;
    }
}
