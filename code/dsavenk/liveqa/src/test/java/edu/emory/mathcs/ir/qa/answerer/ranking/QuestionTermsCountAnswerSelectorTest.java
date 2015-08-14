package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import junit.framework.TestCase;

import java.util.Optional;

/**
 * Created by dsavenk on 8/14/15.
 */
public class QuestionTermsCountAnswerSelectorTest extends TestCase {

    public void testChooseBestAnswer() throws Exception {
        Question q = new Question(
                "", "What is the capital of the US?",
                "I knew, just forgot.", "");
        Answer[] answers = new Answer[] {
                new Answer("London is the capital of Great Britain", ""),
                new Answer("Washington is the capital of the US", ""),
                new Answer("I don't know what I'm doing here", "")
        };
        QuestionTermsCountAnswerSelector answerSelector =
                new QuestionTermsCountAnswerSelector();
        Optional<Answer> bestAnswer =
                answerSelector.chooseBestAnswer(q, answers);
        assertTrue(bestAnswer.isPresent());
        assertTrue(bestAnswer.get().getAnswer().text.contains("Washington"));
    }
}