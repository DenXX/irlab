package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.FreebaseEntityAnnotation;
import edu.emory.cqaqa.types.QuestionAnswerPair;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.List;

/**
 * Created by dsavenk on 4/30/14.
 */
public class FilterNotLinkedProcessor implements QuestionAnswerPairProcessor {
    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair qa) {
        for (List<CoreLabel> sentence : qa.getQuestionTokens()) {
            for (CoreLabel word : sentence) {
                if (word.get(FreebaseEntityAnnotation.class) != null) {
                    return qa;
                }
            }
        }
        return null;
    }
}
