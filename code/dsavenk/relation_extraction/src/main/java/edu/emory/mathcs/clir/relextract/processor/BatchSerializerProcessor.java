package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.utils.ProtobufAnnotationSerializer;
import edu.stanford.nlp.pipeline.Annotation;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dsavenk on 9/26/14.
 */
public class BatchSerializerProcessor extends SerializerProcessor {

    private final ProtobufAnnotationSerializer serializer_ =
            new ProtobufAnnotationSerializer(false);
    private OutputStream out_;
    private String outFilename_;
    private int batchIndex;
    private int docCounter = 0;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public BatchSerializerProcessor(Properties properties) throws IOException {
        super(properties);
        outFilename_ = properties.getProperty("output");
        batchIndex = 0;
        createNewOutputStream();
    }

    private void createNewOutputStream() throws IOException {
        if (out_ != null) out_.close();
        out_ = new BufferedOutputStream(new GZIPOutputStream(
                new FileOutputStream(outFilename_ + "_" + batchIndex++)));
    }

    @Override
    protected Annotation doProcess(Annotation document) throws IOException {
        synchronized (this) {
            serializer_.toProto(document).writeDelimitedTo(out_);
            if (++docCounter > 100000) {
                createNewOutputStream();
                docCounter = 0;
            }
        }
        return document;
    }

    @Override
    public void finishProcessing() throws Exception {
        out_.close();
    }
}
