package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.input.YahooAnswersXmlInput;
import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import junit.framework.TestCase;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

import java.io.IOException;
import java.util.Map;

/**
 * Tests the BM25 feature generator.
 */
public class BM25FeatureGeneratorTest extends TestCase {
    private IndexReader reader_;

    private static YahooAnswersXmlInput.QnAPair createQnAPair(
            String title, String body) {
        return new YahooAnswersXmlInput.QnAPair(
                "", title, body, new String[0], "", null);
    }

    public void setUp() throws IOException {
        Directory dir = new RAMDirectory();
        IndexWriter writer = QnAIndexDocument.createIndexWriter(dir);

        writer.addDocument(
                QnAIndexDocument.getIndexDocument(
                        createQnAPair(
                                "Should I kiss my boyfriend?",
                                "I'm just shy")));
        writer.addDocument(
                QnAIndexDocument.getIndexDocument(
                        createQnAPair(
                                "I lost my original copies of my W2 s from " +
                                        "2014 tax return year. Do I need " +
                                        "those for the upcoming tax return " +
                                        "or will copies be okay?",
                                "")));
        writer.addDocument(
                QnAIndexDocument.getIndexDocument(
                        createQnAPair(
                                "I got bit by a small black spider?",
                                "I'm I going to die?")));
        writer.addDocument(
                QnAIndexDocument.getIndexDocument(
                        createQnAPair(
                                "What kind of audio/backing track is one " +
                                        "meant to take to a recording studio?",
                                "I'm just shy")));
        writer.addDocument(
                QnAIndexDocument.getIndexDocument(
                        createQnAPair(
                                "How can I become a president?",
                                "I'm really ambitious")));
        writer.addDocument(
                QnAIndexDocument.getIndexDocument(
                        createQnAPair(
                                "What are my chances to be 5'10?",
                                "What are the chances of 15 a year old male " +
                                        "who is 5'7 to become 5'10 or " +
                                        "taller.")));

        writer.commit();
        writer.close();
        reader_ = DirectoryReader.open(dir);
    }

    public void testGenerateFeatures() throws Exception {
        final FeatureGeneration f = new BM25FeatureGenerator(reader_);
        Question q = new Question(
                "", "Who is the president of the US?", "I mean in 2015", "");
        Answer a = new Answer("I think the president is Barack Obama", "");
        Map<String, Double> features = f.generateFeatures(q, a);

        // Test some features for presence.
        assertEquals(3, features.size());
        assertTrue(features.values().stream().allMatch(x -> x > 0.0));
    }

    public void testGenerateFeaturesScoreBetterForMoreMatches()
            throws Exception {
        final FeatureGeneration f = new BM25FeatureGenerator(reader_);
        Question q = new Question(
                "", "Who is the president of the USA?", "I mean in 2015", "");
        Answer a1 = new Answer("I think the president is Barack Obama", "");
        Answer a2 = new Answer("The president of the USA is Barack Obama", "");
        Map<String, Double> features1 = f.generateFeatures(q, a1);
        Map<String, Double> features2 = f.generateFeatures(q, a2);

        // Check that for the second answer title matches score are greater,
        // body matches are lower, but overall title-body score is again better.
        assertTrue(
                features1.get(
                        BM25FeatureGenerator.TITLE_ANSWER_BM25_FEATURENAME) <
                        features2.get(
                                BM25FeatureGenerator.TITLE_ANSWER_BM25_FEATURENAME));
        assertTrue(features1.containsKey(
                BM25FeatureGenerator.BODY_ANSWER_BM25_ZERO_FEATURENAME));
        assertTrue(features2.containsKey(
                BM25FeatureGenerator.BODY_ANSWER_BM25_ZERO_FEATURENAME));
        assertTrue(features1.get(
                BM25FeatureGenerator.TITLEBODY_ANSWER_BM25_FEATURENAME) <
                features2.get(
                        BM25FeatureGenerator.TITLEBODY_ANSWER_BM25_FEATURENAME
                ));
    }

    public void testGenerateEmptyQuestion() throws Exception {
        final FeatureGeneration f = new BM25FeatureGenerator(reader_);
        Question q = new Question("", "", "", "");
        Answer a = new Answer("I think the president is Barack Obama", "");
        Map<String, Double> features = f.generateFeatures(q, a);
        assertEquals(3, features.size());
        assertTrue(features.containsKey(
                BM25FeatureGenerator.TITLE_ANSWER_BM25_ZERO_FEATURENAME));
        assertTrue(features.containsKey(
                BM25FeatureGenerator.BODY_ANSWER_BM25_ZERO_FEATURENAME));
        assertTrue(features.containsKey(
                BM25FeatureGenerator.TITLEBODY_ANSWER_BM25_ZERO_FEATURENAME));
    }
}