package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.FreebaseEntityAnnotation;
import edu.emory.cqaqa.types.QuestionAnswerPair;
import edu.stanford.nlp.ling.CoreLabel;

import java.util.List;

/**
 * Created by dsavenk on 4/30/14.
 */
public class FilterMultipleEntitiesQuestionsProcessor implements QuestionAnswerPairProcessor {
    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair qa) {
        int count = 0;
        String entity1 = "";
        for (List<CoreLabel> sentence : qa.getQuestionTokens()) {
            for (CoreLabel token : sentence) {
                if (token.get(FreebaseEntityAnnotation.class) != null) {
                    entity1 = token.get(FreebaseEntityAnnotation.class);
                    ++count;
                }
            }
        }
        if (count != 1) {
            return null;
        }
        count = 0;
        String entity2 = "";
        for (List<CoreLabel> sentence : qa.getAnswerTokens()) {
            for (CoreLabel token : sentence) {
                if (token.get(FreebaseEntityAnnotation.class) != null) {
                    entity2 = token.get(FreebaseEntityAnnotation.class);
                    ++count;
                }
            }
        }
        if (count != 1 || entity1.equals(entity2)) {
            return null;
        }
        qa.addAttribute("question_entity", entity1);
        qa.addAttribute("answer_entity", entity2);
        return qa;
    }
}
