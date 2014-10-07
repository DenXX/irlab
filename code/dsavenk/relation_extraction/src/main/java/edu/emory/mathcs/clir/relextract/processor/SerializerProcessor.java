package edu.emory.mathcs.clir.relextract.processor;

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
public class SerializerProcessor extends Processor {

    private final OutputStream out_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public SerializerProcessor(Properties properties) throws IOException {
        super(properties);
        String outFilename = properties.getProperty("output");
        out_ = new BufferedOutputStream(new GZIPOutputStream(
                new FileOutputStream(outFilename)));
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws IOException {
        synchronized (this) {
            document.writeDelimitedTo(out_);
        }
        return document;
    }

    @Override
    public void finishProcessing() throws Exception {
        out_.close();
    }
}
