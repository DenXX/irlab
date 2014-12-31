package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.emory.mathcs.clir.relextract.processor.RelationExtractorTrainEvalProcessor;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.LogPrior;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Generics;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

import java.io.PrintWriter;
import java.util.*;

/**
 * Created by dsavenk on 11/7/14.
 */
public class RelationExtractorModelTrainer {
    public static LinearClassifier<String, Integer> train(Dataset.RelationMentionsDataset trainingDataset, double reg, String optMethod, float negativeWeights, boolean verbose)
            throws Exception {

        Map<Integer, List<String>> featAlphabet = new HashMap<>();
        for (Dataset.Feature feat : trainingDataset.getFeatureList()) {
            if (!featAlphabet.containsKey(feat.getId())) {
                featAlphabet.put(feat.getId(), new ArrayList<>());
            }
            featAlphabet.get(feat.getId()).add(feat.getName());
        }

        edu.stanford.nlp.classify.Dataset<String, Integer> dataset =
                convertDataset(trainingDataset, false, negativeWeights);

        System.out.println("Were feats: " + dataset.numFeatures());
        dataset.applyFeatureCountThreshold(2); //47);
        System.out.println("Now feats: " + dataset.numFeatures());

        // Output counts for all the features if needed.
        if (verbose) {
            outputFeatureCounts(dataset, featAlphabet);
        }

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

        for (String label : trainingDataset.getLabelList()) {
            HashSet<String> labels = new HashSet<>();
            labels.add(label);
            System.out.println("\n\n{{{{{ " + label);
            try {
                for (Triple<Integer, String, Double> feat :
                        model.getTopFeatures(labels, 0.0000001, false, 30, true)) {
                    for (String featName : featAlphabet.get(feat.first)) {
                        System.out.print("(" + featName + ") ");
                    }
                    System.out.println("[" + feat.second + "] = " + feat.third);
                }
            } catch (IllegalArgumentException exc) {

            }
        }

        return model;
    }

    private static void outputFeatureCounts(edu.stanford.nlp.classify.Dataset<String, Integer> dataset,
                                            Map<Integer, List<String>> featAlphabet) {
        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-= Feature Counts : Begin -=-=-=-=-=-=-=-=-=-=-=-=");
        dataset.summaryStatistics();

        Map<Integer, Integer> featureCounts = new HashMap<>();
        Map<String, Map<Integer, Integer>> labelFeatureCounts = new HashMap<>();
        for (int i=0; i < dataset.size(); i++)
        {
            Datum<String, Integer> e = dataset.getDatum(i);
            for (Integer feat : Generics.newHashSet(e.asFeatures())) {
                featureCounts.put(feat, featureCounts.getOrDefault(feat, 0) + 1);
                for (String label : e.labels()) {
                    if (!labelFeatureCounts.containsKey(label)) {
                        labelFeatureCounts.put(label, new HashMap<>());
                    }
                    labelFeatureCounts.get(label).put(feat,
                            labelFeatureCounts.get(label).getOrDefault(feat, 0) + 1);
                }
            }
        }

        System.out.println("--------------\n>>>>> OVERALL: ");
        List<Map.Entry<Integer, Integer>> featureCountsList =
                new ArrayList<>(featureCounts.entrySet());
        printFeatureCounts(featAlphabet, featureCountsList);

        for (String label : labelFeatureCounts.keySet()) {
            System.out.println("--------------\n>>>>> " + label + ": ");
            printFeatureCounts(featAlphabet,
                    new ArrayList<>(labelFeatureCounts.get(label).entrySet()));
        }

        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-= Feature Counts : End -=-=-=-=-=-=-=-=-=-=-=-=");
    }

    private static void printFeatureCounts(Map<Integer, List<String>> featAlphabet, List<Map.Entry<Integer, Integer>> featureCountsList) {
        featureCountsList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        for (Map.Entry<Integer, Integer> featureCount : featureCountsList) {
            System.out.println(featAlphabet.get(
                    featureCount.getKey()).toString() + "\t" + featureCount.getValue());
        }
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
                    if (label.equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL)) {
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
            double bestScore = predictions.getCount(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL);
            String bestLabel = RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL;
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
                                            Dataset.RelationMentionInstance testInstance, boolean verbose) {
        Datum<String, Integer> example = convertTestInstance(testInstance);
        Counter<String> predictions = model.probabilityOf(example);
        double bestScore = predictions.getCount(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL);
        String bestLabel = RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL;
        for (Map.Entry<String, Double> pred : predictions.entrySet()) {
            if (pred.getValue() > bestScore) {
                bestScore = pred.getValue();
                bestLabel = pred.getKey();
            }
        }
        if (verbose) {
            if (!bestLabel.equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL)  ||
                    !testInstance.getLabel(0).equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL)) {
                System.out.println("\n\n======================================\n"
                        + bestLabel + " = " + bestScore);
                System.out.println(testInstance.getMentionText());
                for (Map.Entry<String, Double> score: model.scoresOf(example).entrySet()) {
                    System.out.println(score.getKey() + ": " + score.getValue());
                }
                PrintWriter w =new PrintWriter(System.out);
                model.justificationOf(example, w);
                w.flush();
            }
        }
        return new Pair<>(bestLabel, bestScore);
    }
}
