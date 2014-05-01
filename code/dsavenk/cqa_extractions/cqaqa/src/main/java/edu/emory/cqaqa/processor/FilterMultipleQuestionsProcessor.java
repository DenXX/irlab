package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.QuestionAnswerPair;

/**
 * Created by dsavenk on 4/30/14.
 */
public class FilterMultipleQuestionsProcessor implements QuestionAnswerPairProcessor {
    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair qa) {
        // TODO: Don't know why this is the case at all. May be not best answer.
        if (qa.getQuestionTokens() == null || qa.getAnswerTokens() == null)
            return null;
        if (qa.getAnswerTokens().size() > 1) {
            return null;
        }
        return qa;
    }
}
