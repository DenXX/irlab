package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * A processor that filters out documents which doesn't have at least one entity
 * resolved and if only one entity is resolved then we also need at least one
 * more literal span (number, date, etc).
 */
public class FilterNotresolvedEntitiesProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public FilterNotresolvedEntitiesProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) {
        Set<String> resolvedEntities = new HashSet<>();
        boolean foundLiteral = false;
        for (Document.Span span : document.getSpanList()) {
            if (span.hasEntityId()) {
                resolvedEntities.add(span.getEntityId());
            } else {
                String nerTag = span.getType();
                if (nerTag.equals("DATE") || nerTag.equals("NUMBER") ||
                        nerTag.equals("MONEY") || nerTag.equals("DURATION")) {
                    foundLiteral = true;
                }
            }
        }
        return (resolvedEntities.size() > 1
                || (resolvedEntities.size() == 1 && foundLiteral))
                ? document
                : null;
    }
}
