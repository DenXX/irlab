package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.Properties;

/**
 * Created by dsavenk on 2/2/15.
 */
public class SetAnswerTextAsDocIdProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public SetAnswerTextAsDocIdProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        return document.toBuilder().setDocId(document.getText().substring(document.getQuestionLength(), document.getText().length()).trim()).build();
    }
}
