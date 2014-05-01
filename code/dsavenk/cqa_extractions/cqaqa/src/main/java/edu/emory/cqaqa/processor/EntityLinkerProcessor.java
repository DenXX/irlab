package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.QuestionAnswerPair;
import edu.emory.cqaqa.utils.NlpUtils;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.List;

/**
 * Created by dsavenk on 4/30/14.
 */
public class EntityLinkerProcessor implements QuestionAnswerPairProcessor {
    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair qa) {
        NlpUtils.linkToFreebase(qa.getQuestionTokens());
        NlpUtils.linkToFreebase(qa.getAnswerTokens());
        return qa;
    }
}
