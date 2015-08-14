package edu.emory.mathcs.ir.qa.answerer;

import edu.emory.mathcs.ir.qa.AppConfig;
import junit.framework.TestCase;

/**
 * Tests answer formatter.
 */
public class AnswerFormatterTest extends TestCase {

    public void testFormatLongAnswer() throws Exception {
        final int maximumLength = 20;
        final String longAnswer = "This answer is too long to return.";
        AppConfig.PROPERTIES.setProperty(
                AnswerFormatter.MAXIMUM_ANSWER_LENGTH,
                Integer.toString(maximumLength));
        final String formattedAnswer = AnswerFormatter.formatAnswer(longAnswer);
        assertTrue(formattedAnswer.length() <= maximumLength);
    }
}