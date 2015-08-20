package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import junit.framework.TestCase;

import java.util.Map;

/**
 * Created by dsavenk on 8/19/15.
 */
public class LemmaPairsFeatureGeneratorTest extends TestCase {

    public void testGenerateFeatures() throws Exception {
        final LemmaPairsFeatureGenerator f = new LemmaPairsFeatureGenerator();
        Question q = new Question(
                "", "Who is the president of the US?", "I mean in 2015", "");
        Answer a = new Answer("I think the president is Barack Obama", "");
        Map<String, Double> features = f.generateFeatures(q, a);

        // Test some features for presence.
        assertTrue(features.containsKey("title:who_president"));
        assertTrue(features.containsKey("body:mean_obama"));
        assertTrue(!features.containsKey("title:think_obama"));
        assertTrue(!features.containsKey("body:think_obama"));

        // Features doesn't contain punctuation.
        assertTrue(!features.containsKey("title:?_president"));

        // Features doesn't contain stopwords.
        assertTrue(!features.containsKey("title:president_is"));
    }

    public void testGenerateFeaturesEmptyQuestion() throws Exception {
        final LemmaPairsFeatureGenerator f = new LemmaPairsFeatureGenerator();
        Question q = new Question("", "", "", "");
        Answer a = new Answer("I think the president is Barack Obama", "");
        Map<String, Double> features = f.generateFeatures(q, a);
        assertEquals(0, features.size());
    }
}