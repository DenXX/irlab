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
     * The name of the parameter that stores the name of a file with model to
     * apply.
     */
    public static final String MODEL_PARAMETER = "model";

    /**
     * The name of the parameter to specify whether we need to split all
     * triples into 2 parts for training and validation.
     */
    public static final String SPLIT_TRAIN_TEST_TRIPLES_PARAMETER = "split_triples";

    /**
     * The name of the parameter to specify whether we need to split all
     * triples into 2 parts for training and validation.
     */
    public static final String TYPES_OF_MENTIONS_TO_KEEP_PARAMETER = "mention_types";

    /**
     * A label that means that there is no relations between the given entities.
     */
    public static final String NO_RELATIONS_LABEL = "NONE";

    /**
     * A label that means that we found a relation between the given entities,
     * but it was not one of the active predicates.
     */
    public static final String OTHER_RELATIONS_LABEL = "OTHER";

    public static final int TRAIN_SIZE_FROM_100 = 75;

    private static final float NEGATIVE_SUBSAMPLE_RATE = 0.95f;

    // The list of predicates to build an extractor model for.
    private final Set<String> predicates_;
    private final String datasetOutFilename_;
    private final String modelFilename_;
    private final String[] mentionTypes_;
    private final boolean splitTrainTestTriples_;
    private ConcurrentMap<String, Integer> featureAlphabet_ =
            new ConcurrentHashMap<>();
    private Dataset.RelationMentionsDataset.Builder trainDataset_ =
            Dataset.RelationMentionsDataset.newBuilder();
    private Dataset.RelationMentionsDataset.Builder testDataset_ =
            Dataset.RelationMentionsDataset.newBuilder();
    private LinearClassifier<String, Integer> model_ = null;

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

        // Whether to split all triples into 2 sets for training and testing.
        if (properties.containsKey(SPLIT_TRAIN_TEST_TRIPLES_PARAMETER)) {
            splitTrainTestTriples_ = Boolean.parseBoolean(
                    properties.getProperty(SPLIT_TRAIN_TEST_TRIPLES_PARAMETER));
        } else {
            splitTrainTestTriples_ = true;
        }

        if (properties.containsKey(TYPES_OF_MENTIONS_TO_KEEP_PARAMETER)) {
            mentionTypes_ = properties.getProperty(TYPES_OF_MENTIONS_TO_KEEP_PARAMETER).split(",");
        } else {
            mentionTypes_ = null;
        }

        if (properties.containsKey(MODEL_PARAMETER)) {
            model_ = LinearClassifier.readClassifier(properties.getProperty(MODEL_PARAMETER));
        }
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

        model_ = RelationExtractorModelTrainer.train(trainDataset_.build());
        LinearClassifier.writeClassifier(model_, modelFilename_);

        Dataset.RelationMentionsDataset testDataset = testDataset_.build();

        ArrayList<Pair<String, Double>> predicatedLabels = RelationExtractorModelTrainer.eval(model_, testDataset);
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
                if (triplesLabels.get(preds.getKey()).contains(pred.getKey())) {
                    System.out.println((pred.getKey() + "\t" +
                            preds.getKey().first + "-" + preds.getKey().second + "\t"
                            + pred.getKey() + "\t" + pred.getValue()).replace("\n", " "));
                } else {
                    for (String label : triplesLabels.get(preds.getKey())) {
                        System.out.println(((label.isEmpty() ? "NONE" : label) + "\t" +
                                preds.getKey().first + "-" + preds.getKey().second + "\t"
                                + pred.getKey() + "\t" + pred.getValue()).replace("\n", " "));
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
                ? (document.getDocId().hashCode() % 100) < TRAIN_SIZE_FROM_100
                : (document.getText().hashCode() % 100) < TRAIN_SIZE_FROM_100;

        if (!isInTraining) return;

        Map<Pair<Integer, Integer>, List<Document.Relation>> spans2Labels =
                getSpanPairLabels(document, isInTraining, splitTrainTestTriples_);

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

                        List<String> activeLabels = new ArrayList<>();
                        if (labels != null) {
                            for (Document.Relation label : labels) {
                                if (isPredicateActive(label.getRelation())) {
                                    activeLabels.add(label.getRelation());
                                }
                            }
                        }
                        if (activeLabels.size() == 0) {
                            if (labels == null) {
                                activeLabels.add(NO_RELATIONS_LABEL);
                            } else {
                                activeLabels.add(OTHER_RELATIONS_LABEL);
                            }
                        }

                        // TODO(denxx): This is not correct,
                        // we need to use entityIdIndex, but
                        // currently it is incorrect in the
                        // document.
                        String objectId = objSpan.hasEntityId() ?
                                objSpan.getEntityId() :
                                objSpan.getValue();

                        for (Pair<Integer, Integer> mentionPair :
                                getRelationMentionsIterator(document, subjSpan, objSpan)) {

                            // Do we want to keep this instance, or it should be
                            // removed.
                            if (!keepInstance(subjSpan, mentionPair.first,
                                    objSpan, mentionPair.second,
                                    activeLabels, isInTraining)) {
                                continue;
                            }

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

                            for (String label : activeLabels) {
                                mentionInstance.addLabel(label);

                                // TODO(denxx): This is not correct,
                                // we need to use entityIdIndex, but
                                // currently it is incorrect in the
                                // document.
                                mentionInstance.addTripleBuilder()
                                        .setSubject(subjSpan.getEntityId())
                                        .setObject(objectId)
                                        .setPredicate(label);
                            }

                            // Get ids of all features and add them to the
                            // instance.
                            List<String> features = generateFeatures(document,
                                    subjSpan, mentionPair.first,
                                    objSpan, mentionPair.second);
                            for (String feature : features) {
                                mentionInstance.addFeatureId(
                                        getFeatureId(feature));
                            }

                            processRelationMentionInstance(document, isInTraining, mentionInstance);
                        }
                    }
                }
            }
        }
    }

    private void processRelationMentionInstance(Document.NlpDocument document, boolean isInTraining, Dataset.RelationMentionInstance.Builder mentionInstance) {
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

    private Map<Pair<Integer, Integer>, List<Document.Relation>>
        getSpanPairLabels(Document.NlpDocument document, boolean isInTraining, boolean splitTriples) {

        // Store a mapping from a pair of spans to their relation.
        Map<Pair<Integer, Integer>, List<Document.Relation>> spans2Labels =
                new HashMap<>();
        for (Document.Relation rel : document.getRelationList()) {
            Document.Span objSpan = document.getSpan(rel.getObjectSpan());
            String triple = document.getSpan(rel.getSubjectSpan()).getEntityId() +
                    rel.getRelation()
                    + (objSpan.getType().equals("MEASURE")
                    ? objSpan.getValue()
                    : objSpan.getEntityId());
            // Take positive hashCode of the triple and use it.
            int strTripleHash = triple.hashCode() & 0x7FFFFFFF;

            // We include the label if we don't need to split triples for
            // training and test or if hash parity is appropriate.
            if (!splitTriples
                    || isInTraining == (strTripleHash % 2 == 0)) {
                Pair<Integer, Integer> spans =
                        new Pair<>(rel.getSubjectSpan(), rel.getObjectSpan());
                if (!spans2Labels.containsKey(spans)) {
                    spans2Labels.put(spans, new LinkedList<Document.Relation>());
                }
                spans2Labels.get(spans).add(rel);
            }
        }
        return spans2Labels;
    }

    private boolean keepInstance(Document.Span subjSpan, Integer subjMention,
                                 Document.Span objSpan, Integer objMention,
                                 List<String> activeLabels,
                                 boolean isInTraining) {
        String subjMentionType = subjSpan.getMention(subjMention).getMentionType();
        String objMentionType = objSpan.getMention(objMention).getMentionType();
        boolean subjTypeOk = mentionTypes_ == null;
        boolean objTypeOk = mentionTypes_ == null;
        if (mentionTypes_ != null) {
            for (String type : mentionTypes_) {
                if (subjMentionType.equals(type)) subjTypeOk = true;
                if (objMentionType.equals(type)) objTypeOk = true;
            }
        }

        if (isInTraining) {
            boolean hasRel = !activeLabels.get(0).equals(NO_RELATIONS_LABEL) &&
                    !activeLabels.get(0).equals(OTHER_RELATIONS_LABEL);
            return subjTypeOk && objTypeOk && (hasRel || rnd_.nextFloat() > NEGATIVE_SUBSAMPLE_RATE);
        } else {
            // If mention types are specified as command line parameter, we will
            // use them, otherwise using nominal, pronomial and values.
            boolean subjSpanOk = subjSpan.getType().equals("MEASURE") || subjSpan.getType().equals("ENTITY");
            boolean objSpanOk = objSpan.getType().equals("MEASURE") || objSpan.getType().equals("ENTITY");
            if (mentionTypes_ == null) {
                return subjSpanOk && objSpanOk && (subjMentionType.equals("NOMINAL") || subjMentionType.equals("PRONOMINAL")) &&
                        (objMentionType.equals("NOMINAL") || objMentionType.equals("PRONOMINAL") || objMention.equals("VALUE"));
            } else {
                return subjSpanOk && objSpanOk && subjTypeOk && objTypeOk;
            }
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
        getRelationMentionsIterator(Document.NlpDocument document,
                                Document.Span subjSpan,
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
                || (objSpan.getType().equals("MEASURE")
                    && (objSpan.getNerType().equals("DATE")
                    || objSpan.getNerType().equals("TIME")));
    }
}
