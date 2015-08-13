package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.Optional;

/**
 * An answer ranker, which simply takes the first answer candidate from the
 * list.
 */
public class TopAnswerSelector implements AnswerSelection {
    @Override
    public Optional<Answer> chooseBestAnswer(Question question,
                                             Answer[] answers) {
        // If we don't have any answer candidates we have to return no answer.
        if (answers.length == 0) return Optional.empty();
        return Optional.of(answers[0]);
    }
}
