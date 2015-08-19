package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.Map;

/**
 * Created by dsavenk on 8/19/15.
 */
public interface FeatureGeneration {
    Map<String, Double> generateFeatures(Question question, Answer answer);
}
