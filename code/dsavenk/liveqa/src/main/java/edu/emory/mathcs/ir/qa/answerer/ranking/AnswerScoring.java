package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

/**
 * Interface to score an answer to the given question.
 */
public interface AnswerScoring {
    /**
     * Scores the answer to the given question.
     *
     * @param question The current question.
     * @param answer   The candidate answer to score for the given question.
     * @return double valued score of the answer to the given question.
     */
    double scoreAnswer(Question question, Answer answer);
}
