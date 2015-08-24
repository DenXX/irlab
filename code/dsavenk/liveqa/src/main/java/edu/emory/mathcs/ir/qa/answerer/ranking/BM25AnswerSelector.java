package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.ml.BM25FeatureGenerator;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;
import java.util.Optional;

/**
 * Created by dsavenk on 8/24/15.
 */
public class BM25AnswerSelector implements AnswerSelection {
    private final BM25FeatureGenerator featureGenerator_;

    public BM25AnswerSelector(IndexReader reader) throws IOException {
        featureGenerator_ = new BM25FeatureGenerator(reader);
    }

    @Override
    public Optional<Answer> chooseBestAnswer(Question question, Answer[] answers) {
        double maxScore = 0;
        Optional<Answer> bestAnswer = Optional.empty();
        for (final Answer answer : answers) {
            double score = featureGenerator_.generateFeatures(question, answer)
                    .getOrDefault(BM25FeatureGenerator.TITLEBODY_ANSWER_BM25_FEATURENAME, 0.0);
            if (score > maxScore) {
                maxScore = score;
                bestAnswer = Optional.of(answer);
            }
        }
        return bestAnswer;
    }
}
