package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 1/8/15.
 */
public class FilterNonResolvedSpans extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public FilterNonResolvedSpans(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        docBuilder.clearSpan();
        Map<Integer, Integer> old2new = new HashMap<>();
        int oldIndex = 0, newIndex = 0;
        for (Document.Span span : document.getSpanList()) {
            if (span.getType().equals("MEASURE") || span.hasEntityId()) {
                docBuilder.addSpan(span);
                old2new.put(oldIndex, newIndex);
                ++newIndex;
            }
            ++oldIndex;
        }

        for (Document.Relation.Builder rel : docBuilder.getRelationBuilderList()) {
            rel.setObjectSpan(old2new.get(rel.getObjectSpan()));
            rel.setSubjectSpan(old2new.get(rel.getSubjectSpan()));
        }

        return docBuilder.build();
    }
}
