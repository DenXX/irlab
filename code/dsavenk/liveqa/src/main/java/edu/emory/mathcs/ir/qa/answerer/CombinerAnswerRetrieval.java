package edu.emory.mathcs.ir.qa.answerer;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.Arrays;

/**
 * Answer retrieval that combines candidate answers retrieved by multiple
 * answer retrieval strategies.
 */
public class CombinerAnswerRetrieval implements AnswerRetrieval {
    private final AnswerRetrieval[] answerRetrievals_;

    /**
     * Creates combiner answer retrieval that takes several existing answer
     * retrieval strategies to combine.
     *
     * @param answerRetrievals The AnswerRetrieval strategies to combine.
     */
    public CombinerAnswerRetrieval(AnswerRetrieval... answerRetrievals) {
        answerRetrievals_ = answerRetrievals;
    }

    @Override
    public Answer[] retrieveAnswers(Question question) {
        return Arrays.stream(answerRetrievals_)
                .parallel()
                .flatMap(ar -> Arrays.stream(ar.retrieveAnswers(question)))
                .toArray(Answer[]::new);
    }
}
