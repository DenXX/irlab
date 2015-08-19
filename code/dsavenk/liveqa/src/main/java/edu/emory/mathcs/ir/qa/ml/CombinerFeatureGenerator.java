package edu.emory.mathcs.ir.qa.ml;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Combines features produced by a set of feature generators.
 */
public class CombinerFeatureGenerator implements FeatureGeneration {
    private FeatureGeneration[] featureGenerators;

    /**
     * Creates a combiner feature generator with the given set of feature
     * generators to combine.
     * @param featureGenerators Feature generators to combine.
     */
    public CombinerFeatureGenerator(FeatureGeneration... featureGenerators) {
        this.featureGenerators = featureGenerators;
    }

    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        return Arrays.stream(this.featureGenerators)
                .map(generator -> generator.generateFeatures(question, answer))
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue, Double::sum));
    }
}
