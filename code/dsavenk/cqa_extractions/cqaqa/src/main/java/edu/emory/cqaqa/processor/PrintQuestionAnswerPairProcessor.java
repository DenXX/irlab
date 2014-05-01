package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.QuestionAnswerPair;

/**
 * Created by dsavenk on 4/30/14.
 */
public class PrintQuestionAnswerPairProcessor implements QuestionAnswerPairProcessor {
    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair qa) {
        System.out.println(qa);
        return qa;
    }
}
