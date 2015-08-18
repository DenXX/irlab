package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.input.YahooAnswersXmlInput;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import edu.emory.mathcs.ir.utils.StringUtils;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.optimization.QNMinimizer;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Set;

/**
 * Created by dsavenk on 8/18/15.
 */
public class TrainAnswerSelectionModel {

    public static final int TOPN = 10;

    public static void main(String[] args) {
        final String indexLocation = args[0];
        final String modelLocation = args[1];

        RVFDataset<Boolean, String> dataset = new RVFDataset<>();
        final Directory directory;
        try {
            directory = FSDirectory.open(
                    FileSystems.getDefault().getPath(indexLocation));
            final IndexReader indexReader = DirectoryReader.open(directory);
            final IndexSearcher searcher = new IndexSearcher(indexReader);
            for (int docid = 0; docid < indexReader.maxDoc(); ++docid) {
                final YahooAnswersXmlInput.QnAPair qna =
                        QnAIndexDocument.getQnAPair(
                                indexReader.document(docid));
                RVFDatum<Boolean, String> instance = createInstance(qna, qna);
                instance.setLabel(true);
                dataset.add(instance);

                // Get similar QnA pairs.
                final YahooAnswersXmlInput.QnAPair[] similarQnAPairs =
                        QnAIndexDocument.getSimilarQnAPairs(
                                searcher, qna, TOPN);
                for (final YahooAnswersXmlInput.QnAPair similarQna :
                        similarQnAPairs) {
                    instance = createInstance(qna, similarQna);
                    instance.setLabel(false);
                    dataset.add(instance);
                }

                if (docid > 100) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(dataset.toSummaryString());
        dataset.applyFeatureCountThreshold(2);
        System.err.println(dataset.toSummaryString());

        LinearClassifierFactory<Boolean, String> classifierFactory_ =
                new LinearClassifierFactory<>(1e-4, false, 1.0);
        //classifierFactory_.setTuneSigmaHeldOut();
        classifierFactory_.setMinimizerCreator(() -> {
            QNMinimizer min = new QNMinimizer(15);
            min.useOWLQN(true, 1.0);
            return min;
        });
        classifierFactory_.setVerbose(true);

        LinearClassifier<Boolean, String> model =
                classifierFactory_.trainClassifier(dataset);
        model.saveToFilename(modelLocation);
        System.err.println(
                model.toBiggestWeightFeaturesString(false, 1000, true));
    }

    private static RVFDatum<Boolean, String> createInstance(
            YahooAnswersXmlInput.QnAPair targetQna,
            YahooAnswersXmlInput.QnAPair instanceQna) {
        Counter<String> features = new ClassicCounter<>();

        final Text targetQuestion = new Text(targetQna.questionTitle);
        final Text targetQuestionBody = new Text(targetQna.questionBody);
        final Text answer = new Text(instanceQna.bestAnswer);

        final Set<String> questionTitleTokens = targetQuestion.getLemmas(true);
        final Set<String> questionBodyTokens =
                targetQuestionBody.getLemmas(true);
        final Set<String> answerTokens = answer.getLemmas(true);

        int matchedTitleTerms = 0;
        for (final String questionToken : questionTitleTokens) {
            for (final String answerToken : answerTokens) {
                features.setCount(
                        String.join("_", "title:", questionToken, answerToken),
                        1.0);
                matchedTitleTerms += questionToken.equals(answerToken) ? 1 : 0;
            }
        }
        features.setCount("title_answer_matched_terms", matchedTitleTerms);

        int matchedBodyTerms = 0;
        for (final String questionToken : questionBodyTokens) {
            for (final String answerToken : answerTokens) {
                features.setCount(
                        String.join("_", "body:", questionToken, answerToken),
                        1.0);
                matchedBodyTerms += questionToken.equals(answerToken) ? 1 : 0;
            }
        }
        features.setCount("body_answer_matched_terms", matchedBodyTerms);

        return new RVFDatum<>(features);
    }
}
