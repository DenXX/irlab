package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import junit.framework.TestCase;

import java.util.Map;

/**
 * Created by dsavenk on 8/21/15.
 */
public class AnswerStatsFeatureGeneratorTest extends TestCase {

    public void testGenerateFeatures() throws Exception {
        AnswerStatsFeatureGenerator featureGenerator =
                new AnswerStatsFeatureGenerator();
        final Question q = new Question("", "who is the president of the US?",
                "", "");
        final Answer a = new Answer("I think it is Barack Obama. " +
                "Although I don't care much.", "");
        Map<String, Double> features = featureGenerator.generateFeatures(q, a);
        assertEquals(55.0,
                features.get(AnswerStatsFeatureGenerator.LENGTH_CHAR));
        assertEquals(2.0,
                features.get(AnswerStatsFeatureGenerator.LENGTH_SENTS));
        assertEquals(14.0,
                features.get(AnswerStatsFeatureGenerator.LENGTH_TOKENS));
        assertEquals(7.0,
                features.get(AnswerStatsFeatureGenerator.TOKENS_PER_SENT));
    }
}