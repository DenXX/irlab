package edu.emory.mathcs.clir.relextract.processor;

import java.util.Properties;

/**
 * The processor runs standard Stanford CoreNLP pipeline to tag named entities
 * and resolves them to Freebase using the provided phrase-entity data.
 */
public class EntityAnnotationProcessor extends CoreNlpPipelineProcessor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public EntityAnnotationProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected String getAnnotators() {
        return "tokenize, cleanxml, ssplit, pos, lemma, ner, span, entityres";
    }
}
