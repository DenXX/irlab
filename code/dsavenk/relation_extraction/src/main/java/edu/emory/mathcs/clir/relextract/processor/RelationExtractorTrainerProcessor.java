package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.FileUtils;
import edu.stanford.nlp.util.Pair;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.GZIPOutputStream;

/**
 * Base class for relation extractors, provides functionality to store instances
 * of relation mentions.
 */
public abstract class RelationExtractorTrainerProcessor extends Processor {

    /**
     * The name of the parameter specifying a file with predicates to build
     * extractor for.
     */
    public static final String PREDICATES_LIST_PARAMETER = "predicates";

    /**
     * The name of the parameter specifying a file to store dataset into.
     */
    public static final String DATASET_OUTFILE_PARAMETER = "dataset_file";

    /**
     * A label that means that there is no relations between the given entities.
     */
    public static final String NO_RELATIONS_LABEL = "NONE";

    /**
     * A label that means that we found a relation between the given entities,
     * but it was not one of the active predicates.
     */
    public static final String OTHER_RELATIONS_LABEL = "OTHER"
    // The list of predicates to build an extractor model for.
    private final Set<String> predicates_;
    private final String datasetOutFilename_;
    private ConcurrentMap<String, Integer> featureAlphabet_ =
            new ConcurrentHashMap<>();
    private Dataset.RelationMentionsDataset.Builder mentionsDataset_ =
            Dataset.RelationMentionsDataset.newBuilder();

    /**
     * This class cannot be instantiated, constructor is used by child
     * classes to initialize the list of predicates.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public RelationExtractorTrainerProcessor(Properties properties) throws IOException {
        super(properties);
        predicates_ = new HashSet<>(FileUtils.readLinesFromFile(
                properties.getProperty(PREDICATES_LIST_PARAMETER)));
        datasetOutFilename_ = properties.getProperty(DATASET_OUTFILE_PARAMETER);
    }

    /**
     * Checks if the given predicate is active and we should build an extractor
     * for it.
     *
     * @param predicate A predicate to check.
     * @return True is the given predicate is active.
     */
    public boolean isPredicateActive(String predicate) {
        return predicates_.contains(predicate);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws Exception {
        processSpans(document);
        return document;
    }

    @Override
    public void finishProcessing() throws IOException {
        for (Map.Entry<String, Integer> feature : featureAlphabet_.entrySet()) {
            mentionsDataset_.addFeatureBuilder().setId(feature.getValue());
            mentionsDataset_.addFeatureBuilder().setName(feature.getKey());
        }

        // Write dataset to a file.
        mentionsDataset_.build().writeDelimitedTo(
                new BufferedOutputStream(
                        new GZIPOutputStream(
                                new FileOutputStream(datasetOutFilename_))));
    }

    /**
     * Iterates over pairs of spans, where first span is an entity and the
     * second span is measure or entity.
     *
     * @param document
     */
    protected void processSpans(Document.NlpDocument document) {
        // Store a mapping from a pair of spans to their relation.
        Map<Pair<Integer, Integer>, List<Document.Relation>> spans2Labels =
                new HashMap<>();
        for (Document.Relation rel : document.getRelationList()) {
            Pair<Integer, Integer> spans = new Pair<>(rel.getSubjectSpan(),
                    rel.getObjectSpan());
            if (!spans2Labels.containsKey(spans)) {
                spans2Labels.put(spans, new LinkedList<Document.Relation>());
            }
            spans2Labels.get(spans).add(rel);
        }

        int subjSpanIndex = 0;
        for (Document.Span subjSpan : document.getSpanList()) {
            if (continueWithSubjectSpan(subjSpan)) {
                int objSpanIndex = 0;
                for (Document.Span objSpan : document.getSpanList()) {
                    if (continueWithObjectSpan(subjSpan, objSpan)) {

                        // Get the list of relations between the given entities.
                        List<Document.Relation> labels = spans2Labels.get(
                                new Pair<>(subjSpanIndex, objSpanIndex));

                        for (Pair<Integer, Integer> mentionPair :
                                getRelationMentionsIterator(subjSpan, objSpan)) {
                            Dataset.RelationMentionInstance.Builder mentionInstance =
                                    Dataset.RelationMentionInstance.newBuilder();

                            // Set docid and spans.
                            mentionInstance.setDocId(document.getDocId());
                            mentionInstance.setSubjSpan(subjSpanIndex);
                            mentionInstance.setSubjMention(mentionPair.first);
                            mentionInstance.setObjSpan(objSpanIndex);
                            mentionInstance.setObjMention(mentionPair.second);
                            mentionInstance.setMentionText(
                                    getMentionText(document, subjSpan,
                                            mentionPair.first, objSpan,
                                            mentionPair.second));

                            boolean foundActivePredicate = false;
                            for (Document.Relation label : labels) {
                                if (isPredicateActive(label.getRelation())) {
                                    mentionInstance.addLabel(label.getRelation());
                                    foundActivePredicate = true;
                                }

                                // TODO(denxx): need to attach an actual triple.
                            }
                            // If we didn't find any labels to add we should add
                            // none or other label.
                            if (!foundActivePredicate) {
                                if (labels.size() == 0) {
                                    mentionInstance.addLabel(NO_RELATIONS_LABEL);
                                } else {
                                    mentionInstance.addLabel(OTHER_RELATIONS_LABEL);
                                }
                            }

                            // Get ids of all features and add them to the
                            // instance.
                            for (String feature : generateFeatures(document,
                                    subjSpan, mentionPair.first,
                                    objSpan, mentionPair.second)) {
                                mentionInstance.addFeatureId(
                                        getFeatureId(feature));
                            }

                            synchronized (mentionsDataset_) {
                                mentionsDataset_.addInstance(mentionInstance);
                            }
                        }
                    }
                    ++objSpanIndex;
                }
            }
            ++subjSpanIndex;
        }
    }

    /**
     * Generates a textual representation of the given relation mention.
     *
     * @param document    A document of the relation mention.
     * @param subjSpan    Subject span.
     * @param subjMention Subject span mention.
     * @param objSpan     Object span mention.
     * @param objMention  Object span mention.
     * @return Returns a string representation of the relation mention.
     */
    protected abstract String getMentionText(
            Document.NlpDocument document, Document.Span subjSpan,
            Integer subjMention, Document.Span objSpan, Integer objMention);

    /**
     * Returns the id of the given string feature. Currently hash of the feature
     * is used as id, thus it is not guaranteed to be unique.
     *
     * @param feature The value of the feature.
     * @return The integer Id of the feature.
     */
    private Integer getFeatureId(String feature) {
        return featureAlphabet_.putIfAbsent(feature, feature.hashCode());
    }

    /**
     * Generate features for the given relation mention.
     *
     * @param document    A document relation mention occurs in.
     * @param subjSpan    Index of the subject span.
     * @param subjMention Index of the subject span mention.
     * @param objSpan     Index of the object span.
     * @param objMention  Index of the object span mention.
     * @return A list of string features.
     */
    protected abstract List<String> generateFeatures(
            Document.NlpDocument document,
            Document.Span subjSpan, Integer subjMention,
            Document.Span objSpan, Integer objMention);

    /**
     * Method that returns all relevant relation mentions between the given
     * spans. Should be overridden in children.
     *
     * @param subjSpan Span of the subject of the relation.
     * @param objSpan  Span of the object of the relation.
     * @return Iterable over mention spans.
     */
    protected abstract Iterable<Pair<Integer, Integer>>
    getRelationMentionsIterator(Document.Span subjSpan,
                                Document.Span objSpan);

    /**
     * Checks whether to keep the given span as potential subject span.
     *
     * @param span A span to check.
     * @return True if we should keep this span as potential object span of a
     * relation.
     */
    protected boolean continueWithSubjectSpan(Document.Span span) {
        return span.hasEntityId();
    }

    /**
     * Checks whether to keep the object span or such pair of spans cannot be
     * considered as related.
     *
     * @param subjSpan Already selected subject span.
     * @param objSpan  Object span to check.
     * @return True if the object span can be considered as related to the given
     * subject span.
     */
    protected boolean continueWithObjectSpan(Document.Span subjSpan,
                                             Document.Span objSpan) {
        return (objSpan.hasEntityId() &&
                objSpan.getEntityId() != subjSpan.getEntityId())
                || (objSpan.getType().equals("MEASURE") &&
                (objSpan.getNerType().equals("DATE")
                           || objSpan.getNerType().equals("TIME")));
    }
}
