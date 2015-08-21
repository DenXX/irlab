package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by dsavenk on 8/21/15.
 */
public class AnswerStatsFeatureGenerator implements FeatureGeneration {
    public static final String LENGTH_CHAR = "length_char";
    public static final String LENGTH_TOKENS = "length_tokens";
    public static final String LENGTH_SENTS = "length_sents";
    public static final String TOKENS_PER_SENT = "tokens_per_sent";

    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        Map<String, Double> features = new HashMap<>();

        features.put(LENGTH_CHAR, (double) answer.getAnswer().text.length());
        features.put(LENGTH_SENTS,
                (double) answer.getAnswer().getSentences().length);

        // Count the number of tokens.
        final long tokens = answer.getAnswer().getTokens().length;
        features.put(LENGTH_TOKENS, (double) tokens);
        if (answer.getAnswer().getSentences().length > 0) {
            features.put(TOKENS_PER_SENT,
                    1.0 * tokens / answer.getAnswer().getSentences().length);
        }
        return features;
    }
}
