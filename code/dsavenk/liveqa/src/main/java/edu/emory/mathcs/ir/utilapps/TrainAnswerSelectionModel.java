package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.input.YahooAnswersXmlInput;
import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import edu.emory.mathcs.ir.qa.ml.*;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.optimization.QNMinimizer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by dsavenk on 8/18/15.
 */
public class TrainAnswerSelectionModel {

    public static final int TOPN = 10;

    private static FeatureGeneration featureGenerator;

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

            // Create feature generator.
            featureGenerator = getFeatureGenerator(indexReader);

            for (int docid = 0; docid < indexReader.maxDoc(); ++docid) {
                final YahooAnswersXmlInput.QnAPair qna =
                        QnAIndexDocument.getQnAPair(
                                indexReader.document(docid));
                RVFDatum<Boolean, String> instance = createInstance(qna, qna);
                instance.setLabel(true);
                dataset.add(instance);

                try {
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

                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }

                if (docid % 100 == 0) {
                    System.err.println(
                            String.format("%d qna processed", docid));
                }
                if (docid > 100000) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        System.err.println(dataset.toSummaryString());
        dataset.applyFeatureCountThreshold(10);
        System.err.println(dataset.toSummaryString());

        LinearClassifierFactory<Boolean, String> classifierFactory_ =
                new LinearClassifierFactory<>(1e-4, false, 0.1);
        //classifierFactory_.setTuneSigmaCV(10);
        //classifierFactory_.setRetrainFromScratchAfterSigmaTuning(true);
        classifierFactory_.setMinimizerCreator(() -> {
            QNMinimizer min = new QNMinimizer(15);
            min.useOWLQN(true, 0.1);
            return min;
        });
        classifierFactory_.setVerbose(true);

        LinearClassifier<Boolean, String> model =
                classifierFactory_.trainClassifier(dataset);
        LinearClassifier.writeClassifier(model, modelLocation);
        Set<Boolean> positive = new HashSet<>();
        positive.add(true);
        System.err.println(
                model.getTopFeatures(positive, 0.0001, false, 1000, true));
    }

    private static FeatureGeneration getFeatureGenerator(
            IndexReader indexReader) throws IOException {
        return new CombinerFeatureGenerator(
                new LemmaPairsFeatureGenerator(),
                new BM25FeatureGenerator(indexReader),
                new NamedEntityTypesFeatureGenerator()
        );
    }

    private static RVFDatum<Boolean, String> createInstance(
            YahooAnswersXmlInput.QnAPair targetQna,
            YahooAnswersXmlInput.QnAPair instanceQna) {
        final Question question =
                new Question("", targetQna.questionTitle,
                        targetQna.questionBody, "");
        final Answer answer = new Answer(instanceQna.bestAnswer, "");

        return StanfordClassifierUtils.createInstance(
                featureGenerator.generateFeatures(question, answer));
    }
}
