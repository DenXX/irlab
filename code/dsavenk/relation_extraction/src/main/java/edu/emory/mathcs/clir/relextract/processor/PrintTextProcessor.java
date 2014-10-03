package edu.emory.mathcs.clir.relextract.processor;

import edu.stanford.nlp.pipeline.Annotation;

import java.util.Properties;

/**
 * Created by dsavenk on 9/26/14.
 */
public class PrintTextProcessor extends Processor {
    private int count = 0;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public PrintTextProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Annotation doProcess(Annotation document) throws Exception {
        //System.out.println(document.toString());
        System.out.println(count++);
        return document;
    }
}
