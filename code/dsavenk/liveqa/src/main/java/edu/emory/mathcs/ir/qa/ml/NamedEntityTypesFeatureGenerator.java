package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Feature generator, that extracts features based on types of named entities
 * that occur in the question and answer.
 */
public class NamedEntityTypesFeatureGenerator implements FeatureGeneration {
    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        Set<String> questionTitleLemmas = question.getTitle().getLemmaSet(true);

        // Generate "question title lemma - answer entity type" features.
        Map<String, Double> features =
                Arrays.stream(answer.getAnswer().getEntities())
                        .flatMap(entity -> questionTitleLemmas.stream()
                                .map(lemma -> "title:" + lemma + "_aentity:" +
                                        entity.type))
                        .collect(Collectors.groupingBy(Function.identity(),
                                Collectors.reducing(0.0, e -> 1.0, Double::sum)));

        // Generate "question body lemma - answer entity type" features.
        Set<String> questionBodyLemmas = question.getBody().getLemmaSet(true);
        features.putAll(Arrays.stream(answer.getAnswer().getEntities())
                .flatMap(entity -> questionBodyLemmas.stream()
                        .map(lemma -> "body:" + lemma + "_aentity:" +
                                entity.type))
                .collect(Collectors.groupingBy(Function.identity(),
                        Collectors.reducing(0.0, e -> 1.0, Double::sum))));

        // Generate "question title entity title - answer entity type" features.
        features.putAll(Arrays.stream(answer.getAnswer().getEntities())
                .flatMap(entity ->
                        Arrays.stream(question.getTitle().getEntities())
                                .map(question_entity -> "qtitle_entity:" +
                                        question_entity.type +
                                        "_aentity:" + entity.type))
                .collect(Collectors.groupingBy(Function.identity(),
                        Collectors.reducing(0.0, e -> 1.0, Double::sum))));

        // Generate "question body entity title - answer entity type" features.
        features.putAll(Arrays.stream(answer.getAnswer().getEntities())
                .flatMap(entity ->
                        Arrays.stream(question.getTitle().getEntities())
                                .map(question_entity -> "qbody_entity:" +
                                        question_entity.type +
                                        "_aentity:" + entity.type))
                .collect(Collectors.groupingBy(Function.identity(),
                        Collectors.reducing(0.0, e -> 1.0, Double::sum))));
        return features;
    }
}
