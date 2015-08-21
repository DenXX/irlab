package edu.emory.mathcs.ir.qa.ml;

import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.stats.Counters;

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

    /**
     * Create an instance with the difference of features between instance1 and
     * instance2.
     *
     * @param instance1 The instance to subtract from.
     * @param instance2 The instance to subtract.
     * @param label     The label of the new instance.
     * @param <C>       The type of the class label.
     * @param <F>       The type of feature names.
     * @return New instance with differences between instance1 and instance2
     * features.
     */
    public static <C, F> RVFDatum<C, F> createDiffInstance(
            RVFDatum<C, F> instance1, RVFDatum<C, F> instance2, C label) {
        return new RVFDatum<>(Counters.diff(instance1.asFeaturesCounter(),
                instance2.asFeaturesCounter()), label);
    }
}
