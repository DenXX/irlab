package edu.emory.mathcs.clir.relextract.extraction;

import de.bwaldvogel.liblinear.*;
import edu.emory.mathcs.clir.relextract.data.Dataset;

import java.io.*;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dsavenk on 2/6/15.
 */
public class LibLinearExtractionModel extends ExtractionModel {

    private Model model_;
    private Map<String, Integer> labelsIndexes_;

    public LibLinearExtractionModel() {
    }

    public static LibLinearExtractionModel load(String modelPath) throws Exception {
        LibLinearExtractionModel res = new LibLinearExtractionModel();
        res.model_ = Linear.loadModel(new File(modelPath));
        ObjectInputStream in = new ObjectInputStream(
                new BufferedInputStream(
                        new FileInputStream(modelPath + "_labels")));
        res.labelsIndexes_ = (Map<String, Integer>)in.readObject();
        in.close();
        return res;
    }

    @Override
    public void train(Dataset.RelationMentionsDataset dataset) {
        Parameter param = new Parameter(SolverType.L1R_LR, 1.0, 0.0001);
        model_ = Linear.train(convertDataset(dataset), param);
    }

    @Override
    public Map<String, Double> predict(Dataset.RelationMentionInstance instance) {
        double[] probs = new double[labelsIndexes_.size()];
        Linear.predictProbability(model_, convertFeatures(instance), probs);
        String[] labels = new String[labelsIndexes_.size()];
        labelsIndexes_.entrySet().stream().forEach(x -> labels[x.getValue()] = x.getKey());
        Map<String, Double> scores = new HashMap<>();
        for (int i = 0; i < labels.length; ++i) {
            scores.put(labels[i], probs[i]);
        }
        return scores;
    }

    @Override
    public void save(String modelPath) throws IOException {
        Writer out = new BufferedWriter(new FileWriter(modelPath));
        Linear.saveModel(out, model_);
        out.close();
        ObjectOutputStream labelsOut = new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(modelPath + "_labels")));
        labelsOut.writeObject(labelsIndexes_);
        labelsOut.close();

    }

    private Problem convertDataset(Dataset.RelationMentionsDataset dataset) {
        labelsIndexes_ = new HashMap<>();
        for (int i = 0; i < dataset.getLabelCount(); ++i) {
            labelsIndexes_.put(dataset.getLabel(i), i);
        }
        Problem problem = new Problem();
        problem.n = dataset.getFeatureCount();
        problem.l = dataset.getInstanceCount();
        problem.y = new double[problem.l];
        problem.x = new Feature[problem.l][];
        for (int i = 0; i < dataset.getInstanceCount(); ++i) {
            problem.y[i] = labelsIndexes_.get(dataset.getInstance(i).getLabel(0));
            problem.x[i] = convertFeatures(dataset.getInstance(i));
        }
        return problem;
    }

    private Feature[] convertFeatures(Dataset.RelationMentionInstance instance) {
        Feature[] res = new Feature[instance.getFeatureIdCount()];
        for (int j = 0; j < instance.getFeatureIdCount(); ++j) {
            res[j] = new FeatureNode(instance.getFeatureId(j), 1);
        }
        return res;
    }
}
