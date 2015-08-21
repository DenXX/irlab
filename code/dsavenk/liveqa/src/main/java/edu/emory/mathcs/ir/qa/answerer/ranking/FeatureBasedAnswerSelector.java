package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.ml.FeatureGeneration;
import edu.emory.mathcs.ir.qa.ml.StanfordClassifierUtils;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Candidate answer ranking based on the machine learning feature-based model.
 */
public class FeatureBasedAnswerSelector implements AnswerSelection {
    private final LinearClassifier<Boolean, String> model_;
    private final FeatureGeneration featureGenerator_;

    /**
     * Creates answer selector which uses the model read from the specified
     * location and feature generator to score candidate answers and choose
     * the best.
     *
     * @param modelPath        The path to the model file.
     * @param featureGenerator The feature generator to use.
     */
    public FeatureBasedAnswerSelector(
            String modelPath, FeatureGeneration featureGenerator) {
        this(LinearClassifier.readClassifier(modelPath), featureGenerator);
    }

    /**
     * Creates answer selector which uses the specified model and feature
     * generator to score candidate answers and choose the best.
     *
     * @param model            The model to use for candidate answer ranking.
     * @param featureGenerator The features generator to use for feature
     *                         generation.
     */
    public FeatureBasedAnswerSelector(
            LinearClassifier<Boolean, String> model,
            FeatureGeneration featureGenerator) {
        model_ = model;
        featureGenerator_ = featureGenerator;
    }

    @Override
    public Optional<Answer> chooseBestAnswer(
            Question question, Answer[] answers) {
        List<AnswerScorePair> answerScores = Arrays.stream(answers)
                .map(answer -> {
                    final Datum<Boolean, String> instance =
                            StanfordClassifierUtils.createInstance(
                                    featureGenerator_.generateFeatures(
                                            question, answer));
                    final double score = model_.scoreOf(instance, true);
                    return new AnswerScorePair(answer, score);
                })
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());

        return answerScores.size() > 0 ?
                Optional.of(answerScores.get(0).answer) : Optional.empty();
    }

    /**
     * Utility class that holds an answer and its score.
     */
    private static class AnswerScorePair
            implements Comparable<AnswerScorePair> {
        public Answer answer;
        public double score;

        public AnswerScorePair(Answer answer, double score) {
            this.answer = answer;
            this.score = score;
        }

        @Override
        public int compareTo(AnswerScorePair o) {
            return Double.compare(this.score, o.score);
        }
    }
}
