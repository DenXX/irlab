package edu.emory.mathcs.ir.qa.ml;

import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;

import java.util.Map;

/**
 * Utility methods that deal with Stanford Classifier instance creation, etc.
 */
public class StanfordClassifierUtils {

    /**
     * Creates Stanford classifier real-valued data point with the given feature
     * values.
     *
     * @param features A map from feature name to its value.
     * @param <C>      The type of class label.
     * @param <F>      The type of feature name.
     * @return An instance of the RVFDatum class.
     */
    public static <C, F> RVFDatum<C, F> createInstance(
            Map<F, Double> features) {
        Counter<F> feats = new ClassicCounter<>();
        features.entrySet().stream().forEach(f -> feats.setCount(
                f.getKey(), f.getValue()));
        return new RVFDatum<>(feats);
    }
}
