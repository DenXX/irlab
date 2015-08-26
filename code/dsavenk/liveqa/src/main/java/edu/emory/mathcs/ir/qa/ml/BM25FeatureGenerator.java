package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.CollectionStatistics;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Outputs BM25 score for answer matching the question.
 */
public class BM25FeatureGenerator implements FeatureGeneration {
    public static final String TITLE_ANSWER_BM25_FEATURENAME =
            "title_answer_bm25";
    public static final String BODY_ANSWER_BM25_FEATURENAME =
            "body_answer_bm25";
    public static final String TITLEBODY_ANSWER_BM25_FEATURENAME =
            "titlebody_answer_bm25";
    public static final String TITLEBODY_ANSWER_BM25_ZERO_FEATURENAME =
            "titlebody_answer_bm25=0";
    public static final String TITLE_ANSWER_BM25_ZERO_FEATURENAME =
            "title_answer_bm25=0";
    public static final String BODY_ANSWER_BM25_ZERO_FEATURENAME =
            "body_answer_bm25=0";

    private static final double K1 = 1.2;
    private static final double B = 0.75;
    private static final double DEFAULT_AVG_ANSWER_LENGTH = 50;
    private static final double EPS = 0.0001;
    private final IndexReader indexReader_;
    private final double averageAnswerLength_;
    private final int docCount_;
    private Map<String, Integer> termFreqCache_ = new HashMap<>();

    /**
     * Creates feature generator given Lucene index reader to get term and
     * document statistics.
     * @param indexReader Lucene IndexReader used to get various term and
     *                    document statistics.
     */
    public BM25FeatureGenerator(IndexReader indexReader) throws IOException {
        indexReader_ = indexReader;
        docCount_ = indexReader_.numDocs();
        IndexSearcher indexSearcher = new IndexSearcher(indexReader_);
        CollectionStatistics collectionStats =
                indexSearcher.collectionStatistics(
                        QnAIndexDocument.ANSWER_FIELD_NAME);
        final long sumTotalTermFreq = collectionStats.sumTotalTermFreq();
        if (sumTotalTermFreq <= 0) {
            averageAnswerLength_ = DEFAULT_AVG_ANSWER_LENGTH;
        } else {
            final long docCount = collectionStats.docCount() == -1 ?
                    collectionStats.maxDoc() : collectionStats.docCount();
            averageAnswerLength_ = 1.0 * sumTotalTermFreq / docCount;
        }
    }

    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        Map<String, Double> features = new HashMap<>();
        final double titleAnswerBm25 =
                getBM25(question.getTitle(), answer.getAnswer());
        if (titleAnswerBm25 > EPS) {
            features.put(TITLE_ANSWER_BM25_FEATURENAME, titleAnswerBm25);
        } else {
            features.put(TITLE_ANSWER_BM25_ZERO_FEATURENAME, 1.0);
        }

        final double bodyAnswerBm25 =
                getBM25(question.getBody(), answer.getAnswer());
        if (bodyAnswerBm25 > EPS) {
            features.put(BODY_ANSWER_BM25_FEATURENAME, bodyAnswerBm25);
        } else {
            features.put(BODY_ANSWER_BM25_ZERO_FEATURENAME, 1.0);
        }

        final double allAnswerBm25 = getBM25(
                question.getTitle().concat(question.getBody()),
                answer.getAnswer());
        if (allAnswerBm25 > EPS) {
            features.put(TITLEBODY_ANSWER_BM25_FEATURENAME, allAnswerBm25);
        } else {
            features.put(TITLEBODY_ANSWER_BM25_ZERO_FEATURENAME, 1.0);
        }
        return features;
    }

    private double getBM25(Text question, Text answer) {
        final Set<String> questionTerms = question.getLemmaSet(false);
        final List<String> answerLemmas = answer.getLemmaList(false);
        final Map<String, Long> answerTerms = answerLemmas
                .stream()
                .collect(Collectors.groupingBy(Function.identity(),
                        Collectors.counting()));
        double bm25 = 0;
        if (questionTerms.size() > 0) {
            for (String term : questionTerms) {
                if (answerTerms.containsKey(term)) {
                    final double denom = answerTerms.get(term) +
                            K1 * (1 - B + B * answerLemmas.size() /
                                    averageAnswerLength_);
                    bm25 += idf(term) * answerTerms.get(term) *
                            (K1 + 1) / denom;
                }
            }
            return bm25 / questionTerms.size();
        }
        return 0;
    }

    private double idf(String term) {
        termFreqCache_.computeIfAbsent(term, this::getTermDocFreq);
        int docFreq = termFreqCache_.get(term);
        return Math.log(1 + (docCount_ - docFreq + 0.5D) / (docFreq + 0.5D));
    }

    private int getTermDocFreq(String termText) {
        try {
            final Term t =
                    new Term(QnAIndexDocument.QTITLEBODY_FIELD_NAME,
                            termText);
            return indexReader_.docFreq(t);
        } catch (IOException e) {
            return 0;
        }
    }
}
