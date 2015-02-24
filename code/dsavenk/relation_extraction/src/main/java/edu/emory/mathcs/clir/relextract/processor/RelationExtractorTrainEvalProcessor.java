package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.extraction.*;
import edu.emory.mathcs.clir.relextract.utils.FileUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

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
    public static final String MODEL_ALGO_PARAMETER = "modelalgo";

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
     */
    public static final String QUESTION_FEATS_PARAMETER = "qfeats";

    public static final String NER_ONLY_PARAMETER = "ner_only";

    /**
     */
    public static final String NEGATIVE_SUBSAMPLE_PARAMETER = "neg_subsample";

    public static final String REGULARIZATION_PARAMETER = "regularization";

    public static final String NEGATIVE_WEIGHTS_PARAMETER = "neg_weight";

    public static final String MIN_FEATURE_COUNT_PARAMETER = "minfeatcount";

    public static final String FEATURE_DICTIONARY_SIZE_PARAMETER = "feats_count";

    public static final String DEBUG_PARAMETER = "debug";

    public static final String SERIALIZED_MODEL_PARAMETER = "model";

    public static final String NOSPLIT_DATASET_PARAMETER = "dataset_nosplit";

    /**
     * A label that means that there is no relations between the given entities.
     */
    public static final String NO_RELATIONS_LABEL = "NONE";

    public static final int TRAIN_SIZE_FROM_100 = 75;

    private final Set<String> typesOfInterest_;

    private float negativeSubsampleRate = 0.99f;

    private float regularization_ = 0.5f;

    private final boolean includeQFeatures;

    private float negativeWeights_ = 1.0f;

    private int featuresDictionarySize = 10000000;

    private boolean debug_ = false;

    private boolean nerOnly_ = false;

    private boolean splitDataset_ = true;

    private String modelType_ = "StanfordL2LogReg";

    // The list of predicates to build an extractor model for.
    private final Set<String> predicates_;
    private final String datasetOutFilename_;
    private final String modelFilename_;
    private final String[] mentionTypes_;
    private final boolean splitTrainTestTriples_;
    private ConcurrentMap<String, Integer> featureAlphabet_ =
            new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, String> featureAlphabet2_ =
            new ConcurrentHashMap<>();
    private Dataset.RelationMentionsDataset.Builder trainDataset_ =
            Dataset.RelationMentionsDataset.newBuilder();
    private ExtractionModel model_ = null;
    private boolean isTraining_;
    private ConcurrentMap<Pair<String, String>, Map<String, Double>> extractedTriples_ = new ConcurrentHashMap<>();
    private ConcurrentMap<Pair<String, String>, Set<String>> triplesLabels_ = new ConcurrentHashMap<>();
    private ConcurrentMap<Integer, Integer> featureCount_ = new ConcurrentHashMap<>();
    private final KnowledgeBase kb_;


    private Random rnd_ = new Random(42);

    /**
     * This class cannot be instantiated, constructor is used by child
     * classes to initialize the list of predicates.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public RelationExtractorTrainEvalProcessor(Properties properties) throws Exception {
        super(properties);
        predicates_ = new HashSet<>(FileUtils.readLinesFromFile(
                properties.getProperty(PREDICATES_LIST_PARAMETER)));
        datasetOutFilename_ = properties.getProperty(DATASET_OUTFILE_PARAMETER);
        modelFilename_ = properties.getProperty(MODEL_OUTFILE_PARAMETER);

        isTraining_ = !properties.containsKey(SERIALIZED_MODEL_PARAMETER);

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

        if (properties.containsKey(MODEL_ALGO_PARAMETER)) {
            modelType_ = properties.getProperty(MODEL_ALGO_PARAMETER);
        }

        model_ = createExtractionModel(modelType_, properties);

        if (properties.containsKey(QUESTION_FEATS_PARAMETER)) {
            includeQFeatures = true;
        } else {
            includeQFeatures = false;
        }

        if (properties.containsKey(NEGATIVE_SUBSAMPLE_PARAMETER)) {
            negativeSubsampleRate = Float.parseFloat(properties.getProperty(NEGATIVE_SUBSAMPLE_PARAMETER));
        }

        if (properties.containsKey(REGULARIZATION_PARAMETER)) {
            regularization_ = Float.parseFloat(properties.getProperty(REGULARIZATION_PARAMETER));
        }

        if (properties.containsKey(NEGATIVE_WEIGHTS_PARAMETER)) {
            negativeWeights_ = Float.parseFloat(properties.getProperty(NEGATIVE_WEIGHTS_PARAMETER));
        }

        if (properties.containsKey(FEATURE_DICTIONARY_SIZE_PARAMETER)) {
            featuresDictionarySize = Integer.parseInt(properties.getProperty(FEATURE_DICTIONARY_SIZE_PARAMETER));
        }

        if (properties.containsKey(NER_ONLY_PARAMETER)) {
            nerOnly_ = Boolean.parseBoolean(properties.getProperty(NER_ONLY_PARAMETER));
        }
        if (properties.containsKey(DEBUG_PARAMETER)) {
            debug_ = Boolean.parseBoolean(properties.getProperty(DEBUG_PARAMETER));
        }
        if (properties.containsKey(MIN_FEATURE_COUNT_PARAMETER)) {
            Parameters.MIN_FEATURE_COUNT = Integer.parseInt(properties.getProperty(MIN_FEATURE_COUNT_PARAMETER));
        }
        if (properties.containsKey(NOSPLIT_DATASET_PARAMETER)) {
            splitDataset_ = false;
        }

        kb_ = KnowledgeBase.getInstance(properties);

        typesOfInterest_ = new HashSet<>();
        for (String pred : predicates_) {
            typesOfInterest_.add(kb_.getPredicateDomainAndRange(pred).first);
            typesOfInterest_.add(kb_.getPredicateDomainAndRange(pred).second);
        }
    }

    private ExtractionModel createExtractionModel(String modelName, Properties props) throws Exception {
        if (props.containsKey(SERIALIZED_MODEL_PARAMETER)) {
            String modelPath = props.getProperty(SERIALIZED_MODEL_PARAMETER);
            switch (modelName) {
                case "StanfordL2LogReg":
                    return CoreNlpL2LogRegressionExtractionModel.load(modelPath);
                case "MIML":
                    return MimlReExtractionModel.load(modelPath);
                case "MIML_local":
                    return MimlReExtractionModel.load(modelPath);
                default:
                    throw new IllegalArgumentException("Unknown model name " + modelName);
            }
        } else {
            File path;
            switch (modelName) {
                case "StanfordL2LogReg":
                    return new CoreNlpL2LogRegressionExtractionModel(Double.parseDouble(props.getProperty(REGULARIZATION_PARAMETER)), Boolean.parseBoolean(props.getProperty(DEBUG_PARAMETER)));
                case "MIML":
                    path = new File(props.getProperty(MODEL_OUTFILE_PARAMETER));
                    return new MimlReExtractionModel(path.getParent(), path.getName(), false);
                case "MIML_local":
                    path = new File(props.getProperty(MODEL_OUTFILE_PARAMETER));
                    return new MimlReExtractionModel(path.getParent(), path.getName(), true);
                default:
                    throw new IllegalArgumentException("Unknown model name " + modelName);
            }
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
        // If model is null, we will predict labels here, otherwise it was
        // done in the main method.
        if (isTraining_) {
            for (Map.Entry<String, Integer> feature : featureAlphabet_.entrySet()) {
                trainDataset_.addFeatureBuilder().setId(feature.getValue())
                        .setName(feature.getKey());
            }

            // Add all possible labels.
            trainDataset_.addLabel(NO_RELATIONS_LABEL);

            // We construct hash map to make sure labels are unique.
            for (String label : new HashSet<>(predicates_)) {
                trainDataset_.addLabel(label);
            }

            // Remove features by count
            for (Dataset.RelationMentionInstance.Builder instance : trainDataset_.getInstanceBuilderList()) {
                List<Integer> features = instance.getFeatureIdList().stream().filter(x -> featureCount_.get(x) >= Parameters.MIN_FEATURE_COUNT).collect(Collectors.toList());
                instance.clearFeatureId().addAllFeatureId(features);
            }

            model_.train(trainDataset_.build());
            model_.save(modelFilename_);
        } else {
            // We are just dumping all predictions and reading them later.
//            for (Map.Entry<Pair<String, String>, Map<String, Double>> preds : extractedTriples_.entrySet()) {
//                if (preds.getValue().size() > 1 && preds.getValue().containsKey("NONE")) {
//                    preds.getValue().remove("NONE");
//                }
//                for (Map.Entry<String, Double> pred : preds.getValue().entrySet()) {
//                    if (triplesLabels_.get(preds.getKey()).contains(pred.getKey())) {
//                        System.out.println((pred.getKey() + "\t" +
//                                preds.getKey().first + "-" + preds.getKey().second + "\t"
//                                + pred.getKey() + "\t" + pred.getValue()).replace("\n", " "));
//                    } else {
//                        // TODO(denxx): This is probably incorrect piece of code.
//                        for (String label : triplesLabels_.get(preds.getKey())) {
//                            if (!preds.getValue().containsKey(label)) {
//                                System.out.println(((label.isEmpty() ? "NONE" : label) + "\t" +
//                                        preds.getKey().first + "-" + preds.getKey().second + "\t"
//                                        + pred.getKey() + "\t" + pred.getValue()).replace("\n", " "));
//                            }
//                        }
//                    }
//                }
//            }
        }
        // Write dataset to a file.
//        trainDataset_.build().writeDelimitedTo(
//                new BufferedOutputStream(
//                        new GZIPOutputStream(
//                                new FileOutputStream(datasetOutFilename_))));
    }

    private void processPrediction(Dataset.RelationMentionInstance instance,
                                   Pair<String, Double> predictedLabel, Map<String, Double> scores) {
        for (Dataset.Triple curTriple : instance.getTripleList()) {
//            Pair<String, String> arguments = new Pair<>(
//                    curTriple.getSubject(), curTriple.getObject());

            // We do not need to store triples that are non-related and
            // classifier predicts the same.
            if (curTriple.getPredicate().equals(NO_RELATIONS_LABEL)
                    && predictedLabel.first.equals(NO_RELATIONS_LABEL)) {
                continue;
            }

            /* Print prediction scores */
            synchronized (this) {
                if (debug_) {
                    System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                }
                for (Dataset.Triple triple : instance.getTripleList()) {
                    System.out.print(triple.getSubject() + "\t" + triple.getPredicate() + "\t" + triple.getObject().replace("\t", " ").replace("\n", " "));
                    System.out.print("\t" + predictedLabel.first + "\t" + predictedLabel.second);
                    for (Map.Entry<String, Double> e : scores.entrySet()) {
                        System.out.print("\t" + e.getKey() + ":" + e.getValue());
                    }
                    System.out.println();
                }
                if (debug_) {
                    System.out.println(instance.getMentionText());
                }
            }

//            if (!triplesLabels_.containsKey(arguments)) {
//                triplesLabels_.put(arguments, new HashSet<>());
//                extractedTriples_.put(arguments, new HashMap<>());
//            }
//            triplesLabels_.get(arguments).add(curTriple.getPredicate());
//            double prevValue = extractedTriples_.get(arguments).containsKey(predictedLabel.first)
//                    ? extractedTriples_.get(arguments).get(predictedLabel.first)
//                    : Double.NEGATIVE_INFINITY;
//            extractedTriples_.get(arguments).put(predictedLabel.first,
//                    Math.max(prevValue, predictedLabel.second));
        }
    }

    /**
     * Iterates over pairs of spans, where first span is an entity and the
     * second span is measure or entity.
     *
     * @param document
     */
    protected void processSpans(Document.NlpDocument document) {
        // Decide where this document is going to go = training of testing.
        boolean isDocumentInTraining = document.hasDocId()
                ? (document.getDocId().hashCode() % 100) < TRAIN_SIZE_FROM_100
                : (document.getText().hashCode() % 100) < TRAIN_SIZE_FROM_100;

        // If model is specified we only keep testing instances and vice versa.
        if (isTraining_ != isDocumentInTraining && splitDataset_) return;

        Map<Pair<Integer, Integer>, List<Triple<String, String, String>>> spans2Labels =
                getSpanPairLabels(document, isTraining_, splitTrainTestTriples_);

        List<Dataset.RelationMentionInstance.Builder> currentDocInstances = new ArrayList<>();

        Set<Integer> seenFeatures = new HashSet<>();

        int subjSpanIndex = -1;
        for (Document.Span subjSpan : document.getSpanList()) {
            ++subjSpanIndex;
            if (continueWithSubjectSpan(subjSpan)) {
                int objSpanIndex = -1;
                for (Document.Span objSpan : document.getSpanList()) {
                    ++objSpanIndex;
                    if (continueWithObjectSpan(subjSpan, objSpan)) {

                        // Get the list of relations between the given entities.
                        List<Triple<String, String, String>> labels = spans2Labels.get(
                                new Pair<>(subjSpanIndex, objSpanIndex));

                        List<Triple<String, String, String>> activeLabels = new ArrayList<>();
                        boolean shouldSkip = false;
                        if (labels != null) {
                            for (Triple<String, String, String> label : labels) {
                                if (label.second.equals("SKIP")) {
                                    shouldSkip = true;
                                    break;
                                }
                                if (isPredicateActive(label.second)) {
                                    activeLabels.add(label);
                                }
                            }
                        }
                        // This is example of a triple that we trained on,
                        // we don't want to treat it as negative, let's just
                        // remove it.
                        if (shouldSkip) {
                            continue;
                        }

                        if (activeLabels.size() == 0) {
                            activeLabels.add(new Triple(
                                    subjSpan.getEntityId(),
                                    NO_RELATIONS_LABEL,
                                    objSpan.hasEntityId()
                                            ? objSpan.getEntityId()
                                            : objSpan.getValue()));
                        }

                        for (Pair<Integer, Integer> mentionPair :
                                getRelationMentionsIterator(document, subjSpan, objSpan)) {

                            // Do we want to keep this instance, or it should be
                            // removed.
                            if (!keepInstance(subjSpan, mentionPair.first,
                                    objSpan, mentionPair.second,
                                    activeLabels, isDocumentInTraining)) {
                                continue;
                            }

                            Dataset.RelationMentionInstance.Builder mentionInstance = null;

                            if (isTraining_) {
                                synchronized (trainDataset_) {
                                    mentionInstance = trainDataset_.addInstanceBuilder();
                                }
                            } else {
                                mentionInstance = Dataset.RelationMentionInstance.newBuilder();
                            }

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

                            for (Triple<String, String, String> label : activeLabels) {
                                mentionInstance.addLabel(label.second);

                                mentionInstance.addTripleBuilder()
                                        .setSubject(label.first)
                                        .setObject(label.third)
                                        .setPredicate(label.second);
                            }

                            // Get ids of all features and add them to the
                            // instance.
                            List<String> features = generateFeatures(document,
                                    subjSpan, mentionPair.first,
                                    objSpan, mentionPair.second);

                            Set<String> subjTypes = kb_.getAllPossibleSpanTypes(subjSpan, EntityRelationsLookupProcessor.MAX_ENTITY_IDS_COUNT)
                                    .stream()
                                    .filter(typesOfInterest_::contains).collect(Collectors.toSet());
                            Set<String> objTypes = null;
                            if (objSpan.getType().equals("MEASURE")) {
                                objTypes = new HashSet<>();
                                objTypes.add("http://rdf.freebase.com/ns/type.datetime");
                            } else {
                                objTypes = kb_.getAllPossibleSpanTypes(objSpan, EntityRelationsLookupProcessor.MAX_ENTITY_IDS_COUNT)
                                        .stream()
                                        .filter(typesOfInterest_::contains).collect(Collectors.toSet());
                            }

                            for (String subjType : subjTypes) {
                                for (String objType : objTypes) {
                                    features.add("ARGUMENTS_FINE_TYPES:\t" + subjType + " - " + objType);
                                }
                            }

                            for (String feature : features) {
                                if (!includeQFeatures && feature.startsWith("QUESTION_"))
                                    continue;
                                int featureId = getFeatureId(feature);
                                mentionInstance.addFeatureId(featureId);
                                seenFeatures.add(featureId);
                            }

                            processRelationMentionInstance(document, isDocumentInTraining, mentionInstance);
                            currentDocInstances.add(mentionInstance);
                        }
                    }
                }
            }
        }

        for (Dataset.RelationMentionInstance.Builder mentionInstance : currentDocInstances) {
            mentionInstance.setWeight(1.0 / currentDocInstances.size());
        }

        for (int feature : seenFeatures) {
            featureCount_.put(feature, featureCount_.getOrDefault(feature, 0) + 1);
        }
    }

    private void processRelationMentionInstance(Document.NlpDocument document,
                                                boolean isInTraining,
                                                Dataset.RelationMentionInstance.Builder mentionInstance) {
        if (isInTraining) {
            if (debug_) {
                if (!mentionInstance.getLabel(0).equals(NO_RELATIONS_LABEL)) {
                    synchronized (this) {
                        System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-");
                        System.out.println(mentionInstance.getMentionText());
                        for (Dataset.Triple triple : mentionInstance.getTripleList()) {
                            System.out.println(triple.getSubject() + "\t" + triple.getPredicate() + "\t" + triple.getObject());
                        }
                    }
                }
            }
        } else {
            Dataset.RelationMentionInstance mention = mentionInstance.build();
            Map<String, Double> scores = model_.predict(mentionInstance.build());
            if (scores.isEmpty()) {
                scores.put(NO_RELATIONS_LABEL, 1.0);
            }
            Pair<String, Double> prediction = findBest(scores);
            if (!prediction.first.equals(NO_RELATIONS_LABEL)) {
                KnowledgeBase.Triple triple = kb_.getTypeCompatibleTripleOrNull(document,
                        mentionInstance.getSubjSpan(),
                        mentionInstance.getObjSpan(),
                        prediction.first,
                        EntityRelationsLookupProcessor.MAX_ENTITY_IDS_COUNT);
                if (triple == null) return;
                mentionInstance.getTripleBuilderList().stream()
                        .filter(tripleBuilder -> tripleBuilder.getPredicate().equals(NO_RELATIONS_LABEL))
                        .forEach(tripleBuilder -> {
                            tripleBuilder.setSubject(triple.subject);
                            tripleBuilder.setObject(triple.object);
                        });
                mention = mentionInstance.build();
            }

            processPrediction(mention, prediction, scores);
        }
    }

    private Pair<String, Double> findBest(Map<String, Double> scores) {
        Map.Entry<String, Double> e = scores.entrySet().stream().collect(
                Collectors.maxBy((x, y) -> x.getValue().compareTo(y.getValue()))).get();
        return new Pair<>(e.getKey(), e.getValue());
    }

    private void printMentionInstance(Dataset.RelationMentionInstanceOrBuilder instance) {
        System.out.println(instance.getMentionText());
        for (Dataset.Triple triple : instance.getTripleList()) {
            System.out.println(triple.getSubject() + "\t" + triple.getPredicate() + "\t" + triple.getObject());
        }

        for (int featureId : instance.getFeatureIdList()) {
            System.out.println(featureId + " = " + featureAlphabet2_.get(featureId));
        }
        System.out.println("__________________________________________________");
    }

    private Map<Pair<Integer, Integer>, List<Triple<String, String, String>>>
        getSpanPairLabels(Document.NlpDocument document, boolean isInTraining, boolean splitTriples) {

        // Store a mapping from a pair of spans to their relation.
        Map<Pair<Integer, Integer>, List<Triple<String, String, String>>> spans2Labels =
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

            // Get subject span entity id.
            String subject = document.getSpan(rel.getSubjectSpan())
                    .getCandidateEntityId(rel.getSubjectSpanCandidateEntityIdIndex() - 1);
            String object = !document.getSpan(rel.getObjectSpan()).hasEntityId()
                    ? document.getSpan(rel.getObjectSpan()).getValue()
                    : document.getSpan(rel.getObjectSpan())
                    .getCandidateEntityId(rel.getObjectSpanCandidateEntityIdIndex() - 1);

            Pair<Integer, Integer> spans = new Pair<>(rel.getSubjectSpan(), rel.getObjectSpan());
            if (!spans2Labels.containsKey(spans)) {
                spans2Labels.put(spans, new ArrayList<>());
            }

            if (!splitTriples || isInTraining == (strTripleHash % 2 == 0)) {
                spans2Labels.get(spans).add(new Triple(subject, rel.getRelation(), object));
            } else {
                // It is not fair to consider all examples we trained on as
                // incorrect. Let's rather filter them later.
                spans2Labels.get(spans).add(new Triple(subject, "SKIP", object));
            }
        }
        return spans2Labels;
    }

    private boolean keepInstance(Document.Span subjSpan, Integer subjMention,
                                 Document.Span objSpan, Integer objMention,
                                 List<Triple<String, String, String>> activeLabels,
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
            boolean hasRel = !activeLabels.get(0).second.equals(NO_RELATIONS_LABEL);
            return subjTypeOk && objTypeOk && (hasRel || rnd_.nextFloat() > negativeSubsampleRate);
        } else {
            return subjTypeOk && objTypeOk;
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
        if (featuresDictionarySize == -1) {
            featureAlphabet_.putIfAbsent(feature, feature.hashCode() & 0x7FFFFFFF);
        } else {
            featureAlphabet_.putIfAbsent(feature, (feature.hashCode() & 0x7FFFFFFF) % featuresDictionarySize);
        }
        featureAlphabet2_.putIfAbsent(featureAlphabet_.get(feature), feature);
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
        return span.hasEntityId() && span.getCandidateEntityScore(0) > 0.05;
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
        if ((objSpan.hasEntityId() &&
                !objSpan.getEntityId().equals(subjSpan.getEntityId())
                && objSpan.getCandidateEntityScore(0) > 0.05)
                || ("MEASURE".equals(objSpan.getType())
                    && ("DATE".equals(objSpan.getNerType())
                    || "TIME".equals(objSpan.getNerType())))) {

            if (nerOnly_ && (subjSpan.getType().equals("OTHER") ||
                    objSpan.getType().equals("OTHER"))) {
                return false;
            }
            return true;

            // Commenting this out for now...
//            for (String pred : predicates_) {
//                if (kb_.hasTypeCompatibleTriple(subjSpan, objSpan, pred,
//                        EntityRelationsLookupProcessor.MAX_ENTITY_IDS_COUNT)) {
//                    return true;
//                }
//            }
        }
        return false;
    }
}
