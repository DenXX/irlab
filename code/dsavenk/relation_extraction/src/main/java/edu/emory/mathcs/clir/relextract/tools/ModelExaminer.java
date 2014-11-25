package edu.emory.mathcs.clir.relextract.tools;

import edu.stanford.nlp.classify.LinearClassifier;

/**
 * Created by dsavenk on 11/25/14.
 */
public class ModelExaminer {
    public static void main(String[] args) {
        LinearClassifier<String, Integer> model = LinearClassifier.readClassifier(args[0]);
        System.out.println(model.topFeaturesToString(model.getTopFeatures(0.00001, true, 1000)));

    }
}
