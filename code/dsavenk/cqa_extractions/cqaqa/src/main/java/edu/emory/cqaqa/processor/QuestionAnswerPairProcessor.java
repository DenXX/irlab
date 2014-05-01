package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.QuestionAnswerPair;

/**
 * Is used by parser for a callback.
 */
public interface QuestionAnswerPairProcessor {

    public QuestionAnswerPair processPair(QuestionAnswerPair qa);
}
