package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.FileUtils;
import edu.emory.mathcs.clir.relextract.utils.RelationExtractorModelTrainer;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.util.Pair;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Base class for relation extractors, provides functionality to store instances
 * of relation mentions.
 */
public abstract class RelationExtractorTrainEvalProcessor extends Processor {

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
     * The name of the parameter specifying a file to store dataset into.
     */
    public static final String MODEL_OUTFILE_PARAMETER = "model_file";

    /**
     * A label that means that there is no relations between the given entities.
     */
    public static final String NO_RELATIONS_LABEL = "NONE";

    /**
     * A label that means that we found a relation between the given entities,
     * but it was not one of the active predicates.
     */
    public static final String OTHER_RELATIONS_LABEL = "OTHER";
    // The list of predicates to build an extractor model for.
    private final Set<String> predicates_;
    private final String datasetOutFilename_;
    private final String modelFilename_;
    private ConcurrentMap<String, Integer> featureAlphabet_ =
            new ConcurrentHashMap<>();
    private Dataset.RelationMentionsDataset.Builder trainDataset_ =
            Dataset.RelationMentionsDataset.newBuilder();
    private Dataset.RelationMentionsDataset.Builder testDataset_ =
            Dataset.RelationMentionsDataset.newBuilder();

    private Random rnd_ = new Random(42);

    /**
     * This class cannot be instantiated, constructor is used by child
     * classes to initialize the list of predicates.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public RelationExtractorTrainEvalProcessor(Properties properties) throws IOException {
        super(properties);
        predicates_ = new HashSet<>(FileUtils.readLinesFromFile(
                properties.getProperty(PREDICATES_LIST_PARAMETER)));
        datasetOutFilename_ = properties.getProperty(DATASET_OUTFILE_PARAMETER);
        modelFilename_ = properties.getProperty(MODEL_OUTFILE_PARAMETER);
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
    public void finishProcessing() throws Exception {
        for (Map.Entry<String, Integer> feature : featureAlphabet_.entrySet()) {
            trainDataset_.addFeatureBuilder().setId(feature.getValue())
                    .setName(feature.getKey());
            testDataset_.addFeatureBuilder().setId(feature.getValue())
                    .setName(feature.getKey());
        }

        // Add all possible labels.
        trainDataset_.addLabel(NO_RELATIONS_LABEL);
        trainDataset_.addLabel(OTHER_RELATIONS_LABEL);
        testDataset_.addLabel(NO_RELATIONS_LABEL);
        testDataset_.addLabel(OTHER_RELATIONS_LABEL);

        // We construct hash map to make sure labels are unique.
        for (String label : new HashSet<>(predicates_)) {
            trainDataset_.addLabel(label);
            testDataset_.addLabel(label);
        }

        LinearClassifier<String, Integer> model =
                RelationExtractorModelTrainer.train(trainDataset_.build());
        Dataset.RelationMentionsDataset testDataset = testDataset_.build();
        ArrayList<Pair<String, Double>> predicatedLabels = RelationExtractorModelTrainer.eval(model, testDataset);
        Map<Pair<String, String>, Map<String, Double>> extractedTriples = new HashMap<>();
        Map<Pair<String, String>, Set<String>> triplesLabels = new HashMap<>();
        int index = 0;
        for (Dataset.RelationMentionInstance instance : testDataset.getInstanceList()) {
            for (Dataset.Triple curTriple : instance.getTripleList()) {
                Pair<String, String> arguments = new Pair<>(
                        curTriple.getSubject(), curTriple.getObject());
                if (!triplesLabels.containsKey(arguments)) {
                    triplesLabels.put(arguments, new HashSet<String>());
                    extractedTriples.put(arguments, new HashMap<String, Double>());
                }
                triplesLabels.get(arguments).add(curTriple.getPredicate());
                double prevValue = extractedTriples.get(arguments).containsKey(predicatedLabels.get(index).first)
                        ? extractedTriples.get(arguments).get(predicatedLabels.get(index).first)
                        : Double.NEGATIVE_INFINITY;
                extractedTriples.get(arguments).put(predicatedLabels.get(index).first,
                        Math.max(prevValue, predicatedLabels.get(index).second));
            }
            ++index;
        }

        for (Map.Entry<Pair<String, String>, Map<String, Double>> preds : extractedTriples.entrySet()) {
            if (preds.getValue().size() > 1 && preds.getValue().containsKey("NONE")) {
                preds.getValue().remove("NONE");
            }
            for (Map.Entry<String, Double> pred : preds.getValue().entrySet()) {
                if (!pred.getKey().equals("NONE")) {
                    if (triplesLabels.get(preds.getKey()).contains(pred.getKey())) {
                        System.out.println(pred.getKey() + "\t" +
                                preds.getKey().first + "-" + preds.getKey().second + "\t"
                                + pred.getKey() + "\t" + pred.getValue());
                    } else {
                        for (String label : triplesLabels.get(preds.getKey())) {
                            System.out.println((label.isEmpty() ? "NONE" : label) + "\t" +
                                    preds.getKey().first + "-" + preds.getKey().second + "\t"
                                    + pred.getKey() + "\t" + pred.getValue());
                        }
                    }
                }
            }
        }

        // Write dataset to a file.
//        trainDataset_.build().writeDelimitedTo(
//                new BufferedOutputStream(
//                        new GZIPOutputStream(
//                                new FileOutputStream(datasetOutFilename_))));
    }

    /**
     * Iterates over pairs of spans, where first span is an entity and the
     * second span is measure or entity.
     *
     * @param document
     */
    protected void processSpans(Document.NlpDocument document) {
        // Decide where this document is going to go = training of testing.
        boolean isInTraining = document.hasDocId()
                ? (document.getDocId().hashCode() % 10) < 8
                : (document.getText().hashCode()) % 10 < 8;

        Map<Pair<Integer, Integer>, List<Document.Relation>> spans2Labels =
                getSpanPairLabels(document, isInTraining);

        int subjSpanIndex = -1;
        for (Document.Span subjSpan : document.getSpanList()) {
            ++subjSpanIndex;
            if (continueWithSubjectSpan(subjSpan)) {
                int objSpanIndex = -1;
                for (Document.Span objSpan : document.getSpanList()) {
                    ++objSpanIndex;
                    if (continueWithObjectSpan(subjSpan, objSpan)) {

                        // Get the list of relations between the given entities.
                        List<Document.Relation> labels = spans2Labels.get(
                                new Pair<>(subjSpanIndex, objSpanIndex));

                        // We don't want too many noisy negative examples.
                        if (labels == null &&
                                (subjSpan.getType().equals("OTHER")
                                        || objSpan.getType().equals("OTHER"))) {
                            continue;
                        }

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
                            if (labels != null) {
                                for (Document.Relation label : labels) {
                                    if (isPredicateActive(label.getRelation())) {
                                        mentionInstance.addLabel(label.getRelation());

                                        // TODO(denxx): This is not correct,
                                        // we need to use entityIdIndex, but
                                        // currently it is incorrect in the
                                        // document.
                                        String objectId = objSpan.hasEntityId() ?
                                                objSpan.getEntityId() :
                                                objSpan.getValue();

                                        mentionInstance.addTripleBuilder()
                                                .setSubject(subjSpan.getEntityId())
                                                .setObject(objectId)
                                                .setPredicate(label.getRelation());
                                        foundActivePredicate = true;
                                    }
                                }
                            }
                            // If we didn't find any labels to add we should add
                            // none or other label.
                            if (!foundActivePredicate) {
                                if (labels == null || labels.size() == 0) {
                                    mentionInstance.addLabel(NO_RELATIONS_LABEL);
                                } else {
                                    mentionInstance.addLabel(OTHER_RELATIONS_LABEL);
                                }

                                // We still want to set subject and object and
                                // keep relation empty, so we can use this
                                // information for aggregation instead of
                                // looking through original document.
                                String objectId = objSpan.hasEntityId()
                                        ? objSpan.getEntityId()
                                        : objSpan.getValue();

                                mentionInstance.addTripleBuilder()
                                        .setSubject(subjSpan.getEntityId())
                                        .setObject(objectId);
                            }

                            if (keepInstance(mentionInstance, isInTraining)) {

                                // Get ids of all features and add them to the
                                // instance.
                                List<String> features = generateFeatures(document,
                                        subjSpan, mentionPair.first,
                                        objSpan, mentionPair.second);
                                for (String feature : features) {
                                    mentionInstance.addFeatureId(
                                            getFeatureId(feature));
                                }

//                            System.out.println("\n\n--------------------------");
//                            System.out.println(mentionInstance.getMentionText().replace("\n", " "));
//                            System.out.println("SUBJ: " + subjSpan.getText() + "[" + subjSpan.getEntityId() + "]");
//                            System.out.println("OBJ: " + objSpan.getText() + "[" + objSpan.getEntityId() + "]");
//                            System.out.println("Label: " + mentionInstance.getLabel(0));
//                            for (String featureId : features) {
//                                System.out.println(featureId);
//                            }

                                if (isInTraining) {
                                    synchronized (trainDataset_) {
                                        trainDataset_.addInstance(mentionInstance);
                                    }
                                } else {
                                    synchronized (testDataset_) {
                                        testDataset_.addInstance(mentionInstance);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private Map<Pair<Integer, Integer>, List<Document.Relation>>
    getSpanPairLabels(Document.NlpDocument document, boolean isInTraining) {
        // Store a mapping from a pair of spans to their relation.
        Map<Pair<Integer, Integer>, List<Document.Relation>> spans2Labels =
                new HashMap<>();
        for (Document.Relation rel : document.getRelationList()) {
            Document.Span objSpan = document.getSpan(rel.getObjectSpan());
            int strTripleHash = (document.getSpan(rel.getSubjectSpan()).getEntityId() +
                    rel.getRelation() + (objSpan.getType().equals("MEASURE")
                    ? objSpan.getValue()
                    : objSpan.getEntityId())).hashCode();

            if ((isInTraining && strTripleHash % 2 == 0)
                    || (!isInTraining && strTripleHash % 2 != 0)) {
                Pair<Integer, Integer> spans = new Pair<>(rel.getSubjectSpan(),
                        rel.getObjectSpan());
                if (!spans2Labels.containsKey(spans)) {
                    spans2Labels.put(spans, new LinkedList<Document.Relation>());
                }
                spans2Labels.get(spans).add(rel);
            }
        }
        return spans2Labels;
    }

    private boolean keepInstance(Dataset.RelationMentionInstanceOrBuilder mentionInstance, boolean isInTraining) {
        String label = mentionInstance.getLabel(0);
        if (OTHER_RELATIONS_LABEL.equals(label) || NO_RELATIONS_LABEL.equals(label)) {
            return rnd_.nextFloat() > 0.95;
        }
        return true;
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
        // We want to have positive Ids only.
        featureAlphabet_.putIfAbsent(feature, feature.hashCode() & 0x7FFFFFFF);
        return featureAlphabet_.get(feature);
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
