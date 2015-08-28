package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerScoring;

import java.util.HashMap;
import java.util.Map;

/**
 * Feature generator that uses the score produced by the provided answer scorer
 * as a feature. This can be used to combine different scoring models.
 */
public class AnswerScorerBasedFeatureGenerator implements FeatureGeneration {
    private final AnswerScoring answerScorer_;
    private final String featureName_;

    /**
     * Creates feature generator which will use the provided answer scorer to
     * generate a feature.
     *
     * @param featureName  The name of the feature to generate;
     * @param answerScorer Answer scorer to use as a feature.
     */
    public AnswerScorerBasedFeatureGenerator(String featureName,
                                             AnswerScoring answerScorer) {
        answerScorer_ = answerScorer;
        featureName_ = featureName;
    }

    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        Map<String, Double> feature = new HashMap<>();
        feature.put(featureName_, answerScorer_.scoreAnswer(question, answer));
        return feature;
    }
}
