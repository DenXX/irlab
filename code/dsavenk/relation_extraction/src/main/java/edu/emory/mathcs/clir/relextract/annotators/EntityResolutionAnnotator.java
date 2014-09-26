package edu.emory.mathcs.clir.relextract.annotators;

import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import edu.stanford.nlp.ling.CoreAnnotation;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;
import edu.stanford.nlp.util.CoreMap;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.zip.GZIPInputStream;

/**
 * Resolves entities found by NER tagger to a knowledge base. The annotator
 * reads a file with.
 */
public class EntityResolutionAnnotator implements Annotator {
    public static final String ANNOTATOR_CLASS = "entityres";
    public static final Requirement ENTITYRES_REQUIREMENT =
            new Requirement(ANNOTATOR_CLASS);
    public static final String LEXICON_PROPERTY =
            "EntityResolutionAnnotator_lexicon_file";
    // A trie, that maps a name to the entity with the best score from the
    // list of available entities. Currently the score is the number of triples
    // available for the entity, so we prefer more "popular" entities.
    private final PatriciaTrie<String> namesIndex_ = new PatriciaTrie<>();

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
    public EntityResolutionAnnotator(String annotatorClass, Properties props)
            throws IOException {
        String lexicon_filename = props.getProperty(LEXICON_PROPERTY);
        System.err.println("Loading entity names ...");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(
                        new FileInputStream(lexicon_filename))));
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            int langPos = fields[0].indexOf("\"@", 1);
            String name = fields[0].substring(1, langPos);
            String lang = fields[0].substring(langPos + 2);
            // Currently we only use English names and aliases.
            if (lang.equals("en")) {
                long bestCount = 0;
                String bestEntity = null;
                for (int i = 2; i < fields.length; i += 2) {
                    long count = Long.parseLong(fields[i]);
                    if (count > bestCount) {
                        bestCount = count;
                        bestEntity = fields[i - 1];
                    }
                }
                // We should have at least one entity and count shouldn't be 0.
                assert bestEntity != null;
                namesIndex_.put(NlpUtils.normalizeStringForMatch(name),
                        bestEntity);
            }
        }
        System.err.println("Done loading entity names.");
    }

    @Override
    public void annotate(Annotation annotation) {
        List<CoreMap> spans = annotation.get(
                SpanAnnotator.SpanAnnotation.class);
        for (CoreMap span : spans) {
            String name = NlpUtils.normalizeStringForMatch(
                    span.get(CoreAnnotations.TextAnnotation.class));
            String nerTag =
                    span.get(CoreAnnotations.NamedEntityTagAnnotation.class);
            if (nerTag.equals("PERSON") || nerTag.equals("ORGANIZATION") ||
                    nerTag.equals("LOCATION") || nerTag.equals("MISC")) {
                if (namesIndex_.containsKey(name)) {
                    span.set(EntityResolutionAnnotation.class,
                            namesIndex_.get(name));
                }
            }
        }
    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(ENTITYRES_REQUIREMENT);
    }

    @Override
    public Set<Requirement> requires() {
        return new ArraySet<>(SpanAnnotator.SPAN_REQUIREMENT);
    }

    /**
     * Annotation class for entity resolution annotations. It is a string, which
     * contains ID of the entity in a KB.
     */
    public static class EntityResolutionAnnotation
            implements CoreAnnotation<String> {
        public Class<String> getType() {
            return String.class;
        }
    }
}
