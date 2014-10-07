package edu.emory.mathcs.clir.relextract.annotators;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Resolves entities found by NER tagger to a knowledge base. The annotator
 * reads a file with.
 */
public class EntityRelationsAnnotator implements Annotator {
    public static final String ANNOTATOR_CLASS = "entityrel";

    public static final Requirement ENTITYRELATIONS_REQUIREMENT =
            new Requirement(ANNOTATOR_CLASS);
    private final KnowledgeBase kb_;

    /**
     * Creates a new instance of the EntityResolutionAnnotator. Names dictionary
     * is read from a file, provided through the
     * EntityResolutionAnnotator.lexicon_file property value.
     *
     * @param annotatorClass Classname of the annotator.
     * @param props          A set of properties. This annotator needs
     *                       EntityResolutionAnnotator.lexicon_file property to store
     *                       the name of the .gz file containing entity names dictionary.
     * @throws IOException
     */
    public EntityRelationsAnnotator(String annotatorClass, Properties props) {
        kb_ = KnowledgeBase.getInstance(props);
    }

    @Override
    public void annotate(Annotation annotation) {
        List<CoreMap> spans =
                annotation.get(SpanAnnotator.SpanAnnotation.class);
        int firstSpanIndex = 0;
        boolean found = false;
        for (CoreMap firstSpan : spans) {
            ++firstSpanIndex;
            if (firstSpan.containsKey(
                    EntityResolutionAnnotator.EntityResolutionAnnotation.class)) {
                final String firstMid =
                        firstSpan.get(EntityResolutionAnnotator
                                .EntityResolutionAnnotation.class);
                int secondSpanIndex = 0;
                for (CoreMap secondSpan : spans) {
                    ++secondSpanIndex;
                    if (secondSpan.containsKey(
                            EntityResolutionAnnotator
                                    .EntityResolutionAnnotation.class)) {
                        final String secondMid =
                                secondSpan.get(EntityResolutionAnnotator
                                        .EntityResolutionAnnotation.class);
                        if (firstMid.equals(secondMid)) continue;
                        StmtIterator iter =
                                kb_.getSubjectObjectTriples(firstMid,
                                        secondMid);
                        while (iter.hasNext()) {
                            Statement triple = iter.nextStatement();
                            System.out.println(firstSpan.get(CoreAnnotations.TextAnnotation.class) + " [" + firstMid + "] " +
                                    triple.getPredicate().toString() + " " +
                                    secondSpan.get(CoreAnnotations.TextAnnotation.class) + " [" + secondMid + "] ");
                            found = true;
                        }
                    }
                }
            }
        }
//        if (found) {
//            final String text = annotation.get(CoreAnnotations.TextAnnotation.class);
//            System.out.println(">>> Question:\n" + text.substring(0, ((QuestionAnswerAnnotation) annotation).getQuestionLength()));
//            System.out.println("\n\n>>> Answer:\n" + text.substring(((QuestionAnswerAnnotation) annotation).getQuestionLength()));
//            System.out.println("--------------------------------------------------");
//        }
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(ENTITYRELATIONS_REQUIREMENT);
    }

    @Override
    public Set<Requirement> requires() {
        return new ArraySet<>(EntityResolutionAnnotator.ENTITYRES_REQUIREMENT);
    }

    /**
     * Annotation class for entity resolution annotations. It is a string, which
     * contains ID of the entity in a KB.
     */
    public static class EntityRelationAnnotation
            implements CoreAnnotation<String> {
        public Class<String> getType() {
            return String.class;
        }
    }
}
