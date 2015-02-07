package edu.emory.mathcs.clir.relextract.extraction;

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

import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 11/7/14.
 */
public class CoreNlpL2LogRegressionExtractionModel extends ExtractionModel {

    private double reg_ = 1.0;
    private float negativeWeights_ = 1.f;
    private LinearClassifier<String, Integer> model_;
    private boolean verbose_ = true;

    private CoreNlpL2LogRegressionExtractionModel() {}

    public CoreNlpL2LogRegressionExtractionModel(double reg, boolean verbose) {
        reg_ = reg;
    }

    public static CoreNlpL2LogRegressionExtractionModel load(String modelPath) {
        CoreNlpL2LogRegressionExtractionModel res = new CoreNlpL2LogRegressionExtractionModel();
        res.model_ = LinearClassifier.readClassifier(modelPath);
        return res;
    }

    @Override
    public void train(Dataset.RelationMentionsDataset dataset) {
        Map<Integer, List<String>> featAlphabet = new HashMap<>();
        for (Dataset.Feature feat : dataset.getFeatureList()) {
            if (!featAlphabet.containsKey(feat.getId())) {
                featAlphabet.put(feat.getId(), new ArrayList<>());
            }
            featAlphabet.get(feat.getId()).add(feat.getName());
        }

        edu.stanford.nlp.classify.Dataset<String, Integer> trainingDataset =
                convertDataset(dataset, false, negativeWeights_);

        System.out.println("Number of features: " + trainingDataset.numFeatures());
//        //dataset.applyFeatureCountThreshold(Parameters.MIN_FEATURE_COUNT);
//        System.out.println("Now feats: " + dataset.numFeatures());

        // Output counts for all the features if needed.
//        if (verbose) {
//            outputFeatureCounts(trainingDataset, featAlphabet);
//        }

        LinearClassifierFactory<String, Integer> classifierFactory_ =
                new LinearClassifierFactory<>(1e-4, false, reg_);
        //classifierFactory_.setTuneSigmaHeldOut();
        classifierFactory_.useQuasiNewton();
        classifierFactory_.setVerbose(true);

        model_ = classifierFactory_.trainClassifier(trainingDataset);

        if (verbose_) {
            for (String label : dataset.getLabelList()) {
                HashSet<String> labels = new HashSet<>();
                labels.add(label);
                System.out.println("\n\n{{{{{ " + label);
                try {
                    for (Triple<Integer, String, Double> feat :
                            model_.getTopFeatures(labels, 0.0000001, false, 30, true)) {
                        for (String featName : featAlphabet.get(feat.first)) {
                            System.out.print("(" + featName + ") ");
                        }
                        System.out.println("[" + feat.second + "] = " + feat.third);
                    }
                } catch (IllegalArgumentException exc) {
                }
            }
        }
    }

    @Override
    public Map<String, Double> predict(Dataset.RelationMentionInstance instance) {
        Datum<String, Integer> example = convertTestInstance(instance);
        Counter<String> predictions = model_.probabilityOf(example);
        double bestScore = 1.0 / predictions.size(); // predictions.getCount(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL);
        String bestLabel = RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL;
        for (Map.Entry<String, Double> pred : predictions.entrySet()) {
            if (!pred.getKey().equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL) &&
                    pred.getValue() > bestScore) {
                bestScore = pred.getValue();
                bestLabel = pred.getKey();
            }
        }
        if (verbose_) {
            if (!bestLabel.equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL)  ||
                    !instance.getLabel(0).equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL)) {
                System.out.println("\n\n======================================\n"
                        + bestLabel + " = " + bestScore);
                System.out.println(instance.getMentionText());
                for (Map.Entry<String, Double> score: model_.scoresOf(example).entrySet()) {
                    System.out.println(score.getKey() + ": " + score.getValue());
                }
                PrintWriter w =new PrintWriter(System.out);
                model_.justificationOf(example, w);
                w.flush();
            }
        }
        return predictions.entrySet().stream().collect(Collectors.toMap(key -> key.getKey(), value -> value.getValue()));
    }

    @Override
    public void save(String modelPath) throws IOException {
        LinearClassifier.writeClassifier(model_, modelPath);
    }

    private edu.stanford.nlp.classify.WeightedDataset<String, Integer>
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
                        res.add(instance.getFeatureIdList(), label, instance.hasWeight()
                                ? negativeWeights
                                : (float)instance.getWeight());
                    } else {
                        res.add(instance.getFeatureIdList(), label, instance.hasWeight()
                                ? 1.0f
                                : (float)instance.getWeight());
                    }
                }
            }
        }

        for (Map.Entry<String, Integer> cnt : count.entrySet()) {
            System.out.println(cnt.getKey() + " => " + cnt.getValue());
        }

        return res;
    }

    private Datum<String, Integer> convertTestInstance(
            Dataset.RelationMentionInstance instance) {
        return new BasicDatum<>(instance.getFeatureIdList(), "");
    }

    private void outputFeatureCounts(edu.stanford.nlp.classify.Dataset<String, Integer> dataset,
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

        for (String label : labelFeatureCounts.keySet()) {
            System.out.println("--------------\n>>>>> " + label + ": ");
            printFeatureCounts(featAlphabet, label, labelFeatureCounts, featureCounts);
        }

        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-= Feature Counts : End -=-=-=-=-=-=-=-=-=-=-=-=");
    }

    private void printFeatureCounts(
            Map<Integer, List<String>> featAlphabet,
            String label,
            Map<String, Map<Integer, Integer>> labelFeatureCountsList,
            Map<Integer, Integer> overallFeatureCounts) {
        List<Map.Entry<Integer, Integer>> featureCountsList = new ArrayList<>(labelFeatureCountsList.get(label).entrySet());
        featureCountsList.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
        for (Map.Entry<Integer, Integer> featureCount : featureCountsList) {
            System.out.print(featureCount.getKey() + " ");
            System.out.print(featAlphabet.get(
                    featureCount.getKey()).toString() + "\t" + featureCount.getValue());
            System.out.print(" < overall: " + overallFeatureCounts.get(featureCount.getKey()) + " (" + 1.0 * featureCount.getValue() / overallFeatureCounts.get(featureCount.getKey()) + ") ");
            labelFeatureCountsList.keySet().stream().filter(
                    l -> !l.equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL)
                            && !l.equals(label)
                            && labelFeatureCountsList.get(l).containsKey(featureCount.getKey()))
                    .forEach(l -> System.out.print(label + ": " + labelFeatureCountsList.get(l).get(featureCount.getKey()) + ", "));
            System.out.println(">");
        }
    }
}

