package edu.emory.mathcs.clir.relextract.extraction;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.emory.mathcs.clir.relextract.processor.RelationExtractorTrainEvalProcessor;
import edu.stanford.nlp.ie.machinereading.structure.RelationMention;
import edu.stanford.nlp.kbp.slotfilling.classify.JointBayesRelationExtractor;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 11/7/14.
 */
public class MimlReExtractionModel extends ExtractionModel {

    private JointBayesRelationExtractor model_;
    private String workDir_;
    private String modelFile_;

    public MimlReExtractionModel(String workDir, String modelFile) {
        workDir_ = workDir;
        modelFile_ = modelFile;
    }

    private Properties getModelProperties(String workDir, String model) {
        Properties props = new Properties();
        props.setProperty("work.dir", workDir);
        props.setProperty("serializedRelationExtractorPath", model);

        props.setProperty("trainer.model", "jointbayes");
        props.setProperty("epochs", "5");
        props.setProperty("folds", "5");
        props.setProperty("filter", "all");
        props.setProperty("featureCountThreshold", "1");
        props.setProperty("inference.type", "stable");
        props.setProperty("features", "1");
        props.setProperty("trainy", "true");
        return props;
    }

    public static MimlReExtractionModel load(String path) throws IOException, ClassNotFoundException {
        File modelFile = new File(path);
        MimlReExtractionModel res = new MimlReExtractionModel(modelFile.getParent(), modelFile.getName() + "_y");
        res.model_ = (JointBayesRelationExtractor)JointBayesRelationExtractor.load(path, res.getModelProperties(res.workDir_, res.modelFile_));
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

        System.err.println("Converting dataset...");
        edu.stanford.nlp.kbp.slotfilling.classify.MultiLabelDataset trainingDataset =
                convertDataset(dataset, false);
        System.err.println("Done converting dataset...");

        System.out.println("Number of features: " + trainingDataset.numFeatures());
//        dataset.applyFeatureCountThreshold(Parameters.MIN_FEATURE_COUNT);
//        System.out.println("Now feats: " + dataset.numFeatures());

        System.err.println("Creating trainer...");
        model_ = new JointBayesRelationExtractor(
                getModelProperties(workDir_, modelFile_));
        System.err.println("Start training...");
        model_.train(trainingDataset);
        System.err.println("Done training...");
    }

    @Override
    public Map<String, Double> predict(Dataset.RelationMentionInstance instance) {
        List<Collection<String>> example = new ArrayList<>();
        example.add(convertTestInstance(instance));
        Counter<String> predictions = model_.classifyMentions(example);

        return predictions.entrySet().stream().collect(Collectors.toMap(
                e -> e.getKey().equals(RelationMention.UNRELATED) ? RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL : e.getKey(),
                Map.Entry::getValue));
    }

    @Override
    public void save(String modelPath) throws IOException {
        model_.save(modelPath);
    }

    private edu.stanford.nlp.kbp.slotfilling.classify.MultiLabelDataset convertDataset(Dataset.RelationMentionsDataset dataset, boolean ignoreLabels) {
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
            for (Dataset.RelationMentionInstance instance : instances) {
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
                res.add(labels,
                        dataset.getLabelList().stream().filter(x -> !labels.contains(x) && !x.equals(RelationExtractorTrainEvalProcessor.NO_RELATIONS_LABEL)).collect(Collectors.toSet()),
                        features);
            }
        }

        for (Map.Entry<String, Integer> cnt : count.entrySet()) {
            System.out.println(cnt.getKey() + " => " + cnt.getValue());
        }

        return res;
    }

    private Collection<String> convertTestInstance(
            Dataset.RelationMentionInstance instance) {
        return instance.getFeatureIdList().stream().map((Integer c) -> c.toString()).collect(Collectors.toList());
    }
}
