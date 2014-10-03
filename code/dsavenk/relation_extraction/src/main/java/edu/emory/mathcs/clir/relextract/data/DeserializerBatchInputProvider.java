package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.utils.CoreNLPProtos;
import edu.emory.mathcs.clir.relextract.utils.ProtobufAnnotationSerializer;
import edu.stanford.nlp.pipeline.Annotation;

import java.io.*;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 9/26/14.
 */
public class DeserializerBatchInputProvider extends InputProvider {

    private int currentBatch_;
    private String inputName_;
    private InputStream input_;
    private ProtobufAnnotationSerializer serializer_ =
            new ProtobufAnnotationSerializer();
    private Annotation currentObject_ = null;

    public DeserializerBatchInputProvider(Properties properties) throws IOException {
        inputName_ = properties.getProperty("input");
        currentBatch_ = 0;
        if (openNextInputStream())
            readInputObject();
    }

    private boolean openNextInputStream() throws IOException {
        if (input_ != null) input_.close();
        String nextBatchName = inputName_ + "_" + currentBatch_;
        if (!new File(nextBatchName).exists()) return false;
        input_ = new BufferedInputStream(
                new GZIPInputStream(new FileInputStream(nextBatchName)));
        ++currentBatch_;
        return true;
    }

    private void readInputObject() {
        try {
            CoreNLPProtos.Document doc =
                    CoreNLPProtos.Document.parseDelimitedFrom(input_);
            currentObject_ = doc != null ? serializer_.fromProto(doc) : null;
            if (currentObject_ == null) {
                if (openNextInputStream()) {
                    readInputObject();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            currentObject_ = null;
        }
    }

    @Override
    public boolean hasNext() {
        return currentObject_ != null;
    }

    @Override
    public Annotation next() {
        Annotation obj = currentObject_;
        readInputObject();
        return obj;
    }
}
