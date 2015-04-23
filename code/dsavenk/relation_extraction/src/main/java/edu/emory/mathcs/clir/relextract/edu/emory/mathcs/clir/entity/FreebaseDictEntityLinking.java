package edu.emory.mathcs.clir.relextract.edu.emory.mathcs.clir.entity;

import edu.stanford.nlp.util.Pair;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 4/23/15.
 */
public class FreebaseDictEntityLinking implements EntityLinking {

    private static final float MATCH_MIN_FRACTION = 0.8f;
    private static final int MAX_IDS_COUNT = 15;
    private static final int MIN_TRIPLES_COUNT = 1;
    private static final float MIN_FRACTION_OF_MAX_SCORE = 0.5f;

    private final IndexSearcher dictSearcher_;
    private final QueryBuilder queryBuilder_ = new QueryBuilder(
            new StandardAnalyzer(CharArraySet.EMPTY_SET));
    private final Sort sort_ = new Sort(SortField.FIELD_SCORE,
            new SortField("triple_count", SortField.Type.LONG, true));

    public FreebaseDictEntityLinking(String indexLocation) throws IOException {
        dictSearcher_ = new IndexSearcher(
                DirectoryReader.open(
                        FSDirectory.open(new File(indexLocation))));
    }

    @Override
    public List<Pair<String, Float>> resolveEntity(String name) {
        ScoreDoc[] docs;
        Query q = queryBuilder_.createMinShouldMatchQuery("name", name, MATCH_MIN_FRACTION);
        if (q == null) return Collections.emptyList();

        TopDocs topDocs;
        try {
            topDocs = dictSearcher_.search(q, MAX_IDS_COUNT, sort_);
        } catch (IOException e) {
            return Collections.emptyList();
        }
        docs = topDocs.scoreDocs;

        Map<String, Float> counts = new HashMap<>();
        long maxCount = 1;
        float maxScore = (docs.length > 0) ? (float)((FieldDoc)docs[0]).fields[0] : 0;
        for (ScoreDoc doc : docs) {
            try {
                org.apache.lucene.document.Document document =
                        dictSearcher_.doc(doc.doc);
                float score = (float)((FieldDoc)doc).fields[0];
                if (score < maxScore * MIN_FRACTION_OF_MAX_SCORE) {
                    // Check length? (1.0 * document.get("name").split("\\s+").length / name.split("\\s+").length < 0.6)
                    break;
                }
                long count = (long)((FieldDoc)doc).fields[1];
                if (count > MIN_TRIPLES_COUNT) {
                    maxCount = Math.max(maxCount, count);
                    String id = document.get("id");
                    counts.put(id, (float) count);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        final long finalMaxCount = maxCount;
        return counts.entrySet()
                .stream()
                .sorted((a1, a2) -> a2.getValue().compareTo(a1.getValue()))
                .map(x -> new Pair<>(x.getKey(), x.getValue() / finalMaxCount))
                .collect(Collectors.toList());
    }
}
