package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

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
    protected Document.NlpDocument doProcess(
            Document.NlpDocument document) throws Exception {

        ++count;
        System.out.println("---------------------------------------------");
        System.out.println(document.getText());
        //System.out.println(new DocumentWrapper(document).toString());
        return document;
    }

    @Override
    public void finishProcessing() {
        System.out.println(count);
    }
}
