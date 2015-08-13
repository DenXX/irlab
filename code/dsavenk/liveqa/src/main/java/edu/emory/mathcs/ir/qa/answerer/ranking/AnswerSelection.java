package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.Optional;

/**
 * An interface that defines a method to select the best answer from the given
 * set of alternatives.
 */
public interface AnswerSelection {
    /**
     * Selects the best answer to given question.
     * @param question The question to be answered.
     * @param answers The candidate answers.
     * @return The best answer selected from the given list of candidates.
     */
    Optional<Answer> chooseBestAnswer(Question question, Answer[] answers);
}
