package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.utils.CoreNLPProtos;
import edu.emory.mathcs.clir.relextract.utils.ProtobufAnnotationSerializer;
import edu.stanford.nlp.pipeline.Annotation;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 9/26/14.
 */
public class DeserializerInputProvider extends InputProvider {

    private final InputStream input_;
    private ProtobufAnnotationSerializer serializer_ =
            new ProtobufAnnotationSerializer();
    private Annotation currentObject_ = null;

    public DeserializerInputProvider(Properties properties) throws IOException {
        input_ = new BufferedInputStream(
                new GZIPInputStream(
                        new FileInputStream(
                                properties.getProperty("input"))));
        readInputObject();
    }

    private void readInputObject() {
        try {
            CoreNLPProtos.Document doc =
                    CoreNLPProtos.Document.parseDelimitedFrom(input_);
            currentObject_ = doc != null ? serializer_.fromProto(doc) : null;
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
