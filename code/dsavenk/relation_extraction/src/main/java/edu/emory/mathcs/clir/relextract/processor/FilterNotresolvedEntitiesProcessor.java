package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.annotators.EntityResolutionAnnotator;
import edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.util.CoreMap;

import java.util.HashSet;
import java.util.List;
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
    protected Annotation doProcess(Annotation document) {
        List<CoreMap> spans = document.get(SpanAnnotator.SpanAnnotation.class);
        Set<String> resolvedEntities = new HashSet<>();
        boolean foundLiteral = false;
        for (CoreMap span : spans) {
            if (span.containsKey(
                    EntityResolutionAnnotator
                            .EntityResolutionAnnotation.class)) {
                resolvedEntities.add(span.get(
                        EntityResolutionAnnotator
                                .EntityResolutionAnnotation.class));
            } else {
                String nerTag = span.get(
                        CoreAnnotations.NamedEntityTagAnnotation.class);
                if (nerTag.equals("DATE") || nerTag.equals("NUMBER") ||
                        nerTag.equals("MONEY") || nerTag.equals("DURATION")) {
                    foundLiteral = true;
                }
            }
        }
        return (spans.size() > 1 || (spans.size() == 1 && foundLiteral))
                ? document
                : null;
    }
}
