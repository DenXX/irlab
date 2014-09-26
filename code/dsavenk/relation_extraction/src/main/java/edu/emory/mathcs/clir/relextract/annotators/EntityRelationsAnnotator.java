package edu.emory.mathcs.clir.relextract.annotators;

import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;

import java.io.IOException;
import java.util.Collections;
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
