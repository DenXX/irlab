package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.process.PTBTokenizer;

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
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        document.getSpanList().stream()
                .filter(span -> !span.getType().equals("MEASURE"))
                .forEach(span -> {
                    System.out.println(PTBTokenizer.ptb2Text(span.getText()));
                    for (Document.Mention mention : span.getMentionList()) {
                        System.out.println(PTBTokenizer.ptb2Text(mention.getText()));
                    }
                });
        return null;
    }

    @Override
    public void finishProcessing() {
    }

}
