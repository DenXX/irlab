package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dsavenk on 11/7/14.
 */
public class RelationExtractorModelTrainer {
    public static LinearClassifier<String, Integer> train(Dataset.RelationMentionsDataset trainingDataset)
            throws Exception {

        Map<Integer, String> featAlphabet = new HashMap<>();
        for (Dataset.Feature feat : trainingDataset.getFeatureList()) {
            featAlphabet.put(feat.getId(), feat.getName());
        }

        edu.stanford.nlp.classify.Dataset<String, Integer> dataset =
                convertDataset(trainingDataset, false);

        LinearClassifierFactory<String, Integer> classifierFactory_ =
                new LinearClassifierFactory<>(1e-4, false, 0.1);
        //classifierFactory_.setTuneSigmaHeldOut();
        //classifierFactory_.useConjugateGradientAscent();

        LinearClassifier<String, Integer> model =
                classifierFactory_.trainClassifier(dataset);

        for (Triple<Integer, String, Double> feat :
                model.getTopFeatures(0.0001, true, 1000)) {
            if (!feat.second.equals("NONE")) {
                System.out.println(feat.first + "(" + featAlphabet.get(feat.first) + ") [" + feat.second + "] = " + feat.third);
            }
        }

        return model;
    }

    private static edu.stanford.nlp.classify.Dataset<String, Integer>
        convertDataset(Dataset.RelationMentionsDataset dataset, boolean ignoreLabels) {
        edu.stanford.nlp.classify.Dataset<String, Integer> res =
                new edu.stanford.nlp.classify.Dataset<>();
        for (Dataset.RelationMentionInstance instance : dataset.getInstanceList()) {
            if (ignoreLabels) {
                res.add(instance.getFeatureIdList(), "", true);
            } else {
                for (String label : instance.getLabelList()) {
                    res.add(instance.getFeatureIdList(), label, true);
                }
            }
        }

        return res;
    }

    private static Datum<String, Integer> convertTestInstance(
            Dataset.RelationMentionInstance instance) {
        return new BasicDatum<>(instance.getFeatureIdList(), "");
    }

    public static ArrayList<Pair<String, Double>> eval(LinearClassifier<String, Integer> model,
                                                       Dataset.RelationMentionsDataset testDataset) {
        ArrayList<Pair<String, Double>> res = new ArrayList<>();

        edu.stanford.nlp.classify.Dataset<String, Integer> dataset = convertDataset(testDataset, true);
        for (Datum<String, Integer> example : dataset) {
            Counter<String> predictions = model.probabilityOf(example);
            double bestScore = predictions.getCount("NONE");
            String bestLabel = "NONE";
            for (Map.Entry<String, Double> pred : predictions.entrySet()) {
                if (pred.getValue() > bestScore) {
                    bestScore = pred.getValue();
                    bestLabel = pred.getKey();
                }
            }
            res.add(new Pair<>(bestLabel, bestScore));
        }
        return res;
    }

    public static Pair<String, Double> eval(LinearClassifier<String, Integer> model,
                                            Dataset.RelationMentionInstance testInstance) {
        Datum<String, Integer> example = convertTestInstance(testInstance);
        Counter<String> predictions = model.probabilityOf(example);
        double bestScore = predictions.getCount("NONE");
        String bestLabel = "NONE";
        for (Map.Entry<String, Double> pred : predictions.entrySet()) {
            if (pred.getValue() > bestScore) {
                bestScore = pred.getValue();
                bestLabel = pred.getKey();
            }
        }
        return new Pair<>(bestLabel, bestScore);
    }
}
