package edu.emory.mathcs.clir.relextract.processor;

import java.util.Properties;

/**
 * The processor runs standard Stanford CoreNLP dependency parsing and
 * coreference resolution.
 */
public class ParsingProcessor extends CoreNlpPipelineProcessor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public ParsingProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected String getAnnotators() {
        return "parse, dcoref";
    }
}
