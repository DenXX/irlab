package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.io.*;
import java.util.Properties;

/**
 * Created by dsavenk on 10/7/14.
 */
public class DumpEntityNamesProcessor extends Processor {

    public static final String ENTITY_NAMES_FILENAME = "entitynames_out";

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public DumpEntityNamesProcessor(Properties properties) throws FileNotFoundException {
        super(properties);
        out_ = new PrintWriter(new BufferedOutputStream(
                new FileOutputStream(properties.getProperty(
                        ENTITY_NAMES_FILENAME))));

    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        for (Document.Span span : document.getSpanList()) {
            switch (span.getType()) {
                case "PERSON":
                case "LOCATION":
                case "ORGANIZATION":
                case "MISC":
                    synchronized (out_) {
                        out_.println(span.getText());
                    }
                    break;
            }
        }
        return null;
    }

    @Override
    public void finishProcessing() {
        out_.close();
    }

    private final PrintWriter out_;
}
