package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.emory.mathcs.clir.relextract.processor.RelationExtractorTrainEvalProcessor;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.kbp.slotfilling.classify.JointBayesRelationExtractor;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 11/7/14.
 */
public class MimlReModelTrainer {
    public static JointBayesRelationExtractor train(Dataset.RelationMentionsDataset trainingDataset, String workDir, String model)
            throws Exception {

        Map<Integer, List<String>> featAlphabet = new HashMap<>();
        for (Dataset.Feature feat : trainingDataset.getFeatureList()) {
            if (!featAlphabet.containsKey(feat.getId())) {
                featAlphabet.put(feat.getId(), new ArrayList<>());
            }
            featAlphabet.get(feat.getId()).add(feat.getName());
        }

        System.err.println("Converting dataset...");
        edu.stanford.nlp.kbp.slotfilling.classify.MultiLabelDataset dataset =
                convertDataset(trainingDataset, false);
        System.err.println("Done converting dataset...");

        System.out.println("Were feats: " + dataset.numFeatures());
        dataset.applyFeatureCountThreshold(2); //47);
        System.out.println("Now feats: " + dataset.numFeatures());

        System.err.println("Creating trainer...");
        JointBayesRelationExtractor mimlretrainer = new JointBayesRelationExtractor(getModelProperties(workDir, model));
        System.err.println("Start training...");
        mimlretrainer.train(dataset);
        System.err.println("Done training...");
        return mimlretrainer;
    }

    public static Properties getModelProperties(String workDir, String model) {
        Properties props = new Properties();
        props.setProperty("work.dir", workDir);
        props.setProperty("serializedRelationExtractorPath", model);

        props.setProperty("trainer.model", "jointbayes");
        props.setProperty("epochs", "15");
        props.setProperty("folds", "5");
        props.setProperty("filter", "all");
        props.setProperty("featureCountThreshold", "1");
        props.setProperty("inference.type", "stable");
        props.setProperty("features", "1");
        props.setProperty("trainy", "true");
        return props;
    }


    private static edu.stanford.nlp.kbp.slotfilling.classify.MultiLabelDataset convertDataset(Dataset.RelationMentionsDataset dataset, boolean ignoreLabels) {
        edu.stanford.nlp.kbp.slotfilling.classify.MultiLabelDataset<String, String> res =
                new edu.stanford.nlp.kbp.slotfilling.classify.MultiLabelDataset();

        Map<Pair<String, String>, List<Dataset.RelationMentionInstance>> argumentInstances = new HashMap<>();

        // Group relation instances by arguments.
        for (Dataset.RelationMentionInstance instance : dataset.getInstanceList()) {
            for (Dataset.Triple triple : instance.getTripleList()) {
                Pair<String, String> args = new Pair<>(triple.getSubject(), triple.getSubject());
                if (!argumentInstances.containsKey(args)) {
                    argumentInstances.put(args, new ArrayList<>());
                }
                argumentInstances.get(args).add(instance);
            }
        }

        Map<String, Integer> count = new HashMap<>();

        for (List<Dataset.RelationMentionInstance> instances : argumentInstances.values()) {
            Set<String> labels = new HashSet<>();
            List<Collection<String>> features = new ArrayList<>();
            for (Dataset.RelationMentionInstance instance : dataset.getInstanceList()) {
                for (Dataset.Triple triple : instance.getTripleList()) {
                    if (!count.containsKey(triple.getPredicate())) {
                        count.put(triple.getPredicate(), 0);
                    }
                    count.put(triple.getPredicate(), count.get(triple.getPredicate()) + 1);
                    if (!triple.getPredicate().equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL)) {
                        labels.add(triple.getPredicate());
                    }
                }
                Collection<String> curFeatures = new ArrayList<>();
                for (Integer featureId : instance.getFeatureIdList()) {
                    curFeatures.add(featureId.toString());
                }
                features.add(curFeatures);
            }
            if (ignoreLabels) {
                res.add(Collections.emptySet(), Collections.emptySet(), features);
            } else {
                res.add(labels, Collections.emptySet(), features);
            }
        }

        for (Map.Entry<String, Integer> cnt : count.entrySet()) {
            System.out.println(cnt.getKey() + " => " + cnt.getValue());
        }

        return res;
    }

    private static Collection<String> convertTestInstance(
            Dataset.RelationMentionInstance instance) {
        return instance.getFeatureIdList().stream().map((Integer c) -> c.toString()).collect(Collectors.toList());
    }

    public static Pair<String, Double> eval(JointBayesRelationExtractor model,
                                            Dataset.RelationMentionInstance testInstance, boolean verbose) {
        List<Collection<String>> example = new ArrayList<>();
        example.add(convertTestInstance(testInstance));
        Counter<String> predictions = model.classifyMentions(example);
        double bestScore = predictions.getCount(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL);
        String bestLabel = RelationMention.UNRELATED;
        for (Map.Entry<String, Double> pred : predictions.entrySet()) {
            if (pred.getValue() > bestScore) {
                bestScore = pred.getValue();
                bestLabel = pred.getKey();
            }
        }
        if (Objects.equals(bestLabel, RelationMention.UNRELATED))
            bestLabel = RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL;
        return new Pair<>(bestLabel, bestScore);
    }
}
