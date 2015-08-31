package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.yahooanswers.YahooAnswersSimilarQuestionRetrieval;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Created by dsavenk on 8/31/15.
 */
public class CategoryMatchFeatureGenerator implements FeatureGeneration {
    @Override
    public Map<String, Double> generateFeatures(Question question, Answer answer) {
        Map<String, Double> features = new HashMap<>();
        Optional<String> category = answer.getAttribute(
                YahooAnswersSimilarQuestionRetrieval.CATEGORY_ANSWER_ATTRIBUTE);
        if (category.isPresent()) {
            String[] answerCategories = category.get().split("\t");
            String[] questionCategories = question.getCategories();
            boolean matchedCategories = false;
            for (String questionCategory : questionCategories) {
                for (String answerCategory : answerCategories) {
                    features.put("category_" + questionCategory + "_" +
                            answerCategory, 1.0);
                    if (questionCategory.equals(answerCategory)) {
                        matchedCategories = true;
                    }
                }
            }
            if (matchedCategories) {
                features.put("matched_categories", 1.0);
            } else {
                features.put("not_matched_categories", 1.0);
            }
        }
        return features;
    }
}
