package edu.emory.mathcs.clir.relextract.annotators;

import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.pipeline.LabeledChunkIdentifier;
import edu.stanford.nlp.time.TimeAnnotations;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.ErasureUtils;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Annotates document with a collections of spans (each span is an entity or a
 * measure. It provides a higher level of abstraction over tokens.
 */
public class SpanAnnotator implements Annotator {
    // Name of the annotator and its requirement.
    public static final String ANNOTATOR_CLASS = "span";
    public static final Requirement SPAN_REQUIREMENT =
            new Requirement(ANNOTATOR_CLASS);

    /**
     * Annotation class for span annotations. It is a list of CoreMap entries,
     * where each CoreMap entry stores information on a span mentioned in the
     * document.
     */
    public class SpanAnnotation implements CoreAnnotation<List<CoreMap>> {
        public Class<List<CoreMap>> getType() {
            return ErasureUtils.uncheckedCast(List.class);
        }
    }

    public SpanAnnotator(String annotatorClass, Properties props) {}

    @Override
    public void annotate(Annotation annotation) {
        LabeledChunkIdentifier chunker = new LabeledChunkIdentifier();
        List<CoreLabel> tokens = annotation.get(
                CoreAnnotations.TokensAnnotation.class);
        Integer annoTokenBegin = annotation.get(
                CoreAnnotations.TokenBeginAnnotation.class);
        if (annoTokenBegin == null) { annoTokenBegin = 0; }
        List<CoreMap> spans = chunker.getAnnotatedChunks(tokens,
                annoTokenBegin, CoreAnnotations.TextAnnotation.class,
                CoreAnnotations.NamedEntityTagAnnotation.class);

        for (CoreMap span : spans) {
            String nerTag = span.get(
                    CoreAnnotations.NamedEntityTagAnnotation.class);
            CoreLabel curToken =
                    annotation.get(CoreAnnotations.TokensAnnotation.class)
                            .get(span.get(
                                    CoreAnnotations.TokenBeginAnnotation.class));
            if ("NUMBER".equals(nerTag) || "ORDINAL".equals(nerTag)) {
                if (curToken.has(CoreAnnotations.NumericCompositeValueAnnotation
                        .class)) {
                    span.set(CoreAnnotations.ValueAnnotation.class,
                            curToken.get(CoreAnnotations
                                    .NumericCompositeValueAnnotation.class)
                                    .toString());
                }
            } else if ("TIME".equals(nerTag) || "SET".equals(nerTag)
                    || "DATE".equals(nerTag) || "DURATION".equals(nerTag)) {
                if (curToken.has(TimeAnnotations.TimexAnnotation.class)) {
                    span.set(CoreAnnotations.ValueAnnotation.class,
                            curToken.get(
                                    TimeAnnotations.TimexAnnotation.class)
                                    .value());
                }
            }
        }

        annotation.set(SpanAnnotation.class, spans);
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(SPAN_REQUIREMENT);
    }

    @Override
    public Set<Requirement> requires() {
        return new ArraySet<Requirement>(NER_REQUIREMENT);
    }
}
