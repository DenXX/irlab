package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import junit.framework.TestCase;

import java.util.Map;

/**
 * Created by dsavenk on 8/25/15.
 */
public class MatchesFeatureGeneratorTest extends TestCase {

    public void testGenerateFeatures() throws Exception {
        final FeatureGeneration f = new MatchesFeatureGenerator();
        Question q = new Question(
                "", "Who is the president of the US?", "I mean in 2015", "");
        Answer a = new Answer("I think the president is Barack Obama", "");
        Map<String, Double> features = f.generateFeatures(q, a);

        // Test some features for presence.
        assertEquals(1.0, features.get("title:total_matches="));
        assertEquals(1.0, features.get("all:matched_terms_pos=NN="));
        assertEquals(1.0, features.get("title:matched_terms_pos=NN="));
        assertEquals(0.5, features.get("title:%_matched_terms="));
        assertEquals(0.25, features.get("all:%_matched_terms="));
    }
}