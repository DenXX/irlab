package edu.emory.mathcs.clir.relextract.extraction;

import edu.emory.mathcs.clir.relextract.data.Dataset;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Map;

/**
 * Created by dsavenk on 2/6/15.
 */
public abstract class ExtractionModel {

    public abstract void train(Dataset.RelationMentionsDataset dataset);

    public abstract Map<String, Double> predict(Dataset.RelationMentionInstance instance);

    public abstract void save(String modelPath) throws IOException;

}
