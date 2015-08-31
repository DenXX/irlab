package edu.emory.mathcs.ir.qa.answerer;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerSelection;

/**
 * Generic question answerer that takes answer retrieval and answer selector
 * and apply them to get the best candidate answer.
 */
public class GenericQuestionAnswerer implements QuestionAnswering {
    private AnswerSelection answerSelector_;
    private AnswerRetrieval answerRetrieval_;

    /**
     * Creates generic question answerer with the given answer retrieval and
     * selection strategies.
     * @param answerRetrieval The candidate answer retrieval object.
     * @param answerSelector Candidate answer selection object.
     */
    GenericQuestionAnswerer(AnswerRetrieval answerRetrieval,
                            AnswerSelection answerSelector) {
        answerRetrieval_ = answerRetrieval;
        answerSelector_ = answerSelector;
    }

    @Override
    public Answer GetAnswer(Question question) {
        return answerSelector_.chooseBestAnswer(question,
                answerRetrieval_.retrieveAnswers(question))
                .orElse(getDefaultAnswer());
    }

    private static Answer getDefaultAnswer() {
        return new Answer("I don't know :(", "");
    }
}
