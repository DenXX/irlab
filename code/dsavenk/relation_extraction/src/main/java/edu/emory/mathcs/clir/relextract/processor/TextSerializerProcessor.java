package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.AppParameters;
import edu.emory.mathcs.clir.relextract.data.Document;

import java.io.*;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dsavenk on 9/26/14.
 */
public class TextSerializerProcessor extends Processor {

    private final BufferedWriter out_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public TextSerializerProcessor(Properties properties) throws IOException {
        super(properties);
        String outFilename = properties.getProperty(
                AppParameters.OUTPUT_PARAMETER);
        out_ = new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outFilename)));
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws IOException {
        synchronized (this) {
            out_.write(document.getText());
            out_.newLine();
            out_.write("-----------------------------------------------------");
            out_.newLine();
        }
        return document;
    }

    @Override
    public void finishProcessing() throws Exception {
        out_.close();
    }
}
