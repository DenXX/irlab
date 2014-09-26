package edu.emory.mathcs.clir.relextract.processor;

import java.util.Properties;

/**
 * Created by dsavenk on 9/25/14.
 */
public class EntityRelationsProcessor extends CoreNlpPipelineProcessor {

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public EntityRelationsProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected String getAnnotators() {
        return "entityrel";
    }
}
