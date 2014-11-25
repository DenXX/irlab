package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.LogPrior;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dsavenk on 11/7/14.
 */
public class RelationExtractorModelTrainer {
    public static LinearClassifier<String, Integer> train(Dataset.RelationMentionsDataset trainingDataset, double reg, String optMethod, float negativeWeights)
            throws Exception {

        Map<Integer, List<String>> featAlphabet = new HashMap<>();
        for (Dataset.Feature feat : trainingDataset.getFeatureList()) {
            if (!featAlphabet.containsKey(feat.getId())) {
                featAlphabet.put(feat.getId(), new ArrayList<String>());
            }
            featAlphabet.get(feat.getId()).add(feat.getName());
        }

        edu.stanford.nlp.classify.Dataset<String, Integer> dataset =
                convertDataset(trainingDataset, false, negativeWeights);

        LinearClassifierFactory<String, Integer> classifierFactory_ =
                new LinearClassifierFactory<>(1e-4, false, reg);
        //classifierFactory_.setTuneSigmaHeldOut();
        if (optMethod.equals("CG")) {
            classifierFactory_.useConjugateGradientAscent();
        } else if (optMethod.equals("QN")) {
            classifierFactory_.useQuasiNewton();
        }
        //classifierFactory_.setUseSum(true);
        classifierFactory_.setVerbose(true);

        LinearClassifier<String, Integer> model =
                classifierFactory_.trainClassifier(dataset);

        for (Triple<Integer, String, Double> feat :
                model.getTopFeatures(0.0001, true, 1000)) {
            if (!feat.second.equals("NONE")) {
                System.out.print(feat.first + " ");
                for (String featName : featAlphabet.get(feat.first)) {
                    System.out.print("(" + featName + ") ");
                }
                System.out.println("[" + feat.second + "] = " + feat.third);
            }
        }

        return model;
    }

    private static edu.stanford.nlp.classify.WeightedDataset<String, Integer>
        convertDataset(Dataset.RelationMentionsDataset dataset, boolean ignoreLabels, float negativeWeights) {
        edu.stanford.nlp.classify.WeightedDataset<String, Integer> res =
                new edu.stanford.nlp.classify.WeightedDataset<>();
        Map<String, Integer> count = new HashMap<>();
        for (Dataset.RelationMentionInstance instance : dataset.getInstanceList()) {
            if (ignoreLabels) {
                res.add(instance.getFeatureIdList(), "", true);
            } else {
                for (String label : instance.getLabelList()) {
                    if (!count.containsKey(label)) {
                        count.put(label, 0);
                    }
                    count.put(label, count.get(label) + 1);
                    if (label.equals("NONE") || label.equals("OTHER")) {
                        res.add(instance.getFeatureIdList(), label, negativeWeights);
                    } else {
                        res.add(instance.getFeatureIdList(), label, 1.0f);
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> cnt : count.entrySet()) {
            System.out.println(cnt.getKey() + " => " + cnt.getValue());
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

        edu.stanford.nlp.classify.Dataset<String, Integer> dataset = convertDataset(testDataset, true, 1.0f);
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
//        if (!bestLabel.equals("NONE") && !bestLabel.equals("OTHER")) {
//            System.out.println("\n\n\n" + bestLabel + " = " + bestScore);
//            System.out.println(testInstance.getMentionText());
//            for (Map.Entry<String, Double> score: model.scoresOf(example).entrySet()) {
//                System.out.println(score.getKey() + ": " + score.getValue());
//            }
//            model.justificationOf(example, new PrintWriter(System.out));
//        }
        return new Pair<>(bestLabel, bestScore);
    }
}
