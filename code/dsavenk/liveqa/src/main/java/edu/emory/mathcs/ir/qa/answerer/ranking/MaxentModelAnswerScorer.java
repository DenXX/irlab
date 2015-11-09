package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.ml.FeatureGeneration;
import edu.emory.mathcs.ir.qa.ml.StanfordClassifierUtils;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.ling.Datum;

/**
 * Candidate answer scoring based on the machine learning feature-based model.
 */
public class MaxentModelAnswerScorer implements AnswerScoring {
    private final LinearClassifier<Boolean, String> model_;
    private final FeatureGeneration featureGenerator_;

    /**
     * Creates answer scorer which uses the model read from the specified
     * location and feature generator to score candidate answers.
     *
     * @param modelPath        The path to the model file.
     * @param featureGenerator The feature generator to use.
     */
    public MaxentModelAnswerScorer(
            String modelPath, FeatureGeneration featureGenerator) {
        this(LinearClassifier.readClassifier(modelPath), featureGenerator);
    }

    /**
     * Creates answer scorer which uses the specified model and feature
     * generator to score candidate answers.
     *
     * @param model            The model to use for candidate answer scoring.
     * @param featureGenerator The features generator to use for feature
     *                         generation.
     */
    public MaxentModelAnswerScorer(
            LinearClassifier<Boolean, String> model,
            FeatureGeneration featureGenerator) {
        model_ = model;
        featureGenerator_ = featureGenerator;
    }


    @Override
    public double scoreAnswer(Question question, Answer answer) {
        final Datum<Boolean, String> instance =
                StanfordClassifierUtils.createInstance(
                        featureGenerator_.generateFeatures(
                                question, answer));
        double score = model_.scoreOf(instance, true);
        return score;
    }
}
