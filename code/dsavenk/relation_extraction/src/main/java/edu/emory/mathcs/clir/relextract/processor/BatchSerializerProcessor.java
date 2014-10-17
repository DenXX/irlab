package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.AppParameters;
import edu.emory.mathcs.clir.relextract.data.Document;

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

    public static final String BATCH_SIZE_PARAMETER = "batch_size";
    private final int batchSize_;
    private OutputStream out_ = null;
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
        outFilename_ = properties.getProperty(AppParameters.OUTPUT_PARAMETER);
        batchSize_ = Integer.parseInt(
                properties.getProperty(BATCH_SIZE_PARAMETER, "100000"));
        batchIndex = 0;
        createNewOutputStream();
    }

    private void createNewOutputStream() throws IOException {
        if (out_ != null) out_.close();
        out_ = new BufferedOutputStream(new GZIPOutputStream(
                new FileOutputStream(outFilename_ + "_" + batchIndex++)));
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws IOException {
        synchronized (this) {
            document.writeDelimitedTo(out_);
            if (++docCounter > batchSize_) {
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
