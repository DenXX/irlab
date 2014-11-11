package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import weka.classifiers.functions.Logistic;
import weka.core.*;

/**
 * Created by dsavenk on 11/7/14.
 */
public class RelationExtractorModelTrainer {

    public static final int featureAlphabetSize = 1000000;

    public static void train(Dataset.RelationMentionsDataset trainingDataset)
            throws Exception {
        Logistic model = new Logistic();
        model.setDebug(true);
        model.buildClassifier(convertDataset(trainingDataset));
        System.out.println(model.toString());
    }

    private static Instances convertDataset(Dataset.RelationMentionsDataset dataset) {
        FastVector attrInfo = new FastVector(featureAlphabetSize + 1);
        FastVector labels = new FastVector(dataset.getLabelCount());
        for (String label : dataset.getLabelList()) {
            labels.addElement(label);
        }
        Attribute classAttribute = new Attribute("relation", labels);
        attrInfo.addElement(classAttribute);
        for (Dataset.Feature feature : dataset.getFeatureList()) {
            int index = feature.getId() % featureAlphabetSize + 1;
            attrInfo.setElementAt(new Attribute(feature.getName(), index), index);
        }

        Instances instances = new Instances("rel_extract", attrInfo,
                dataset.getInstanceCount());
        instances.setClassIndex(0);

        for (Dataset.RelationMentionInstance instance : dataset.getInstanceList()) {
            Instance wekaInstance = new SparseInstance(featureAlphabetSize + 1);
            wekaInstance.setDataset(instances);
            // TODO(denxx): Since Weka needs only 1 class label, then we take the
            // first label only.
            wekaInstance.setClassValue(instance.getLabel(0));
            for (int feature : instance.getFeatureIdList()) {
                wekaInstance.setValue(getFeatureIndex(feature), 1);
            }
            instances.add(wekaInstance);
        }

        return instances;
    }

    private static int getFeatureIndex(int featureId) {
        return featureId % featureAlphabetSize + 1;
    }
}
