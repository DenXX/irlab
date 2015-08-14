package edu.emory.mathcs.ir.qa.answerer;

import edu.emory.mathcs.ir.qa.AppConfig;
import edu.emory.mathcs.ir.utils.StringUtils;

/**
 * Formats the answer text, i.e. cuts and beautifies its text.
 */
public class AnswerFormatter {

    public static final String MAXIMUM_ANSWER_LENGTH = "MAX_ANSWER_LENGTH";

    /**
     * Cuts the answer to the given length if necessary.
     * @param answer The answer to format.
     * @return The formatted answer text.
     */
    public static String formatAnswer(String answer) {
        answer = StringUtils.normalizeWhitespaces(answer);
        if (answer.length() < Integer.parseInt(
                AppConfig.PROPERTIES.getProperty(MAXIMUM_ANSWER_LENGTH))) {
            return answer;
        }
        return answer.substring(0, Integer.parseInt(
                AppConfig.PROPERTIES.getProperty(MAXIMUM_ANSWER_LENGTH)) - 3) +
                "...";
    }
}
