package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.utils.StringUtils;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Generates a set of question lemma - answer lemma features for the given pair
 * of question and answer.
 */
public class LemmaPairsFeatureGenerator implements FeatureGeneration {
    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        return answer.getAnswer().getLemmaSet(true).stream()
                .flatMap(answerLemma -> Stream.concat(  // join title and body
                        question.getTitle().getLemmaList(true)
                                .stream().map(lemma -> "title:" + lemma),
                        question.getBody().getLemmaList(true)
                                .stream().map(lemma -> "body:" + lemma))
                        .map(questionLemma ->  // make q-a lemma pairs
                                String.join("_", questionLemma, answerLemma)))
                .collect(Collectors.groupingBy(Function.identity(),
                        Collectors.reducing(0.0, e -> 1.0, Double::sum)));
    }
}
