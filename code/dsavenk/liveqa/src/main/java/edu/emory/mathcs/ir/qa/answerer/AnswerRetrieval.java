package edu.emory.mathcs.ir.qa.answerer;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

/**
 * An interface that should be implemented by components that retrieve a set
 * of candidate answers to the question.
 */
public interface AnswerRetrieval {
    /**
     * Retrives a set of candidate answers to the given question.
     * @param question The question to be answered.
     * @return An array of candidate answers.
     */
    Answer[] retrieveAnswers(Question question);
}
