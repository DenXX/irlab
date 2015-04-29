package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.rdf.model.StmtIterator;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import edu.emory.mathcs.clir.representations.WordVec;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dsavenk on 3/20/15.
 */
public class QAModelTrainerProcessor extends Processor {

    private final Dataset<Boolean, Integer> dataset_ = new Dataset<>();
    private final KnowledgeBase kb_;
    private WordVec wordVec_ = null;
    private Map<String, float[]> embeddings = new HashMap<>();

    private Random rnd_ = new Random(42);
    private String modelFile_;
    private LinearClassifier<Boolean, Integer> model_ = null;
    private String datasetFile_;
    private boolean split_ = false;

    private int alphabetSize_ = 10000000;
    private int maxExamplesPerIteration_ = 100;
    private Map<String, Integer> alphabet_ = Collections.synchronizedMap(new HashMap<>());
    private Set<String> predicates_ = new HashSet<>();
    private double subsampleRate_ = 10;
    private boolean debug_ = false;
    private boolean useFineTypes_ = false;
    private boolean partialPredicateNames_ = false;
    private float regularizer_ = 1.f;
    private boolean isTraining_ = true;

    private final Map<String, Double> pRel_ = new HashMap<>();
    private final Map<String, Map<String, Double>> pRelWord_ = new HashMap<>();
    private final Map<String, Double> pWord_ = new HashMap<>();

    public static final String QA_MODEL_PARAMETER = "qa_model_path";
    public static final String QA_DATASET_PARAMETER = "qa_dataset_path";
    public static final String QA_PREDICATES_PARAMETER = "qa_predicates";
    public static final String QA_TEST_PARAMETER = "qa_test";
    public static final String QA_SUBSAMPLE_PARAMETER = "qa_subsample";
    public static final String SPLIT_DATASET_PARAMETER = "qa_split_data";
    public static final String QA_USE_FREEBASE_TYPESFEATURES = "qa_finetypes_features";
    public static final String QA_PARTIAL_PREDICATE_FEATURES_PARAMETER = "qa_partialpredicate_features";
    public static final String QA_RELATION_WORD_DICT_PARAMETER = "qa_relword_dict";
    public static final String QA_DEBUG_PARAMETER = "qa_debug";
    public static final String QA_REGULARIZER_PARAMETER = "qa_regularizer";
    public static final String QA_WORDVEC_PARAMETER = "qa_wordvec";

    BufferedWriter out;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public QAModelTrainerProcessor(Properties properties) {
        super(properties);
        kb_ = KnowledgeBase.getInstance(properties);
        modelFile_ = properties.getProperty(QA_MODEL_PARAMETER);
        if (properties.containsKey(QA_TEST_PARAMETER)) {
            model_ = LinearClassifier.readClassifier(modelFile_);
            isTraining_ = false;
        }
        if (properties.containsKey(SPLIT_DATASET_PARAMETER)) {
            split_ = true;
        }
        if (properties.containsKey(QA_SUBSAMPLE_PARAMETER)) {
            subsampleRate_ = Double.parseDouble(properties.getProperty(QA_SUBSAMPLE_PARAMETER));
        }
        debug_ = properties.containsKey(QA_DEBUG_PARAMETER);
        useFineTypes_ = properties.containsKey(QA_USE_FREEBASE_TYPESFEATURES);
        partialPredicateNames_ = properties.containsKey(QA_PARTIAL_PREDICATE_FEATURES_PARAMETER);
        datasetFile_ = properties.getProperty(QA_DATASET_PARAMETER);

        if (properties.containsKey(QA_REGULARIZER_PARAMETER)) {
            regularizer_ = Float.parseFloat(properties.getProperty(QA_REGULARIZER_PARAMETER));
        }

        if (properties.containsKey(QA_RELATION_WORD_DICT_PARAMETER)) {
            String[] files = properties.getProperty(QA_RELATION_WORD_DICT_PARAMETER).split(",");
            try {
                readRelationWordMapping(files[0], files[1], files[2]);
            } catch (IOException e) {
                e.printStackTrace();
                pRel_.clear();
                pWord_.clear();
                pRelWord_.clear();
            }
        }

        if (properties.containsKey(QA_PREDICATES_PARAMETER)) {
            try {
                BufferedReader input = new BufferedReader(new FileReader(properties.getProperty(QA_PREDICATES_PARAMETER)));
                String line;
                while ((line = input.readLine()) != null) {
                    String[] countPred = line.trim().split(" ");
                    if (Integer.parseInt(countPred[0]) > 100 && !(countPred[1].startsWith("base.") || countPred[1].startsWith("user."))) {
                        predicates_.add(countPred[1]);
                    }
                }
                input.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (properties.containsKey(QA_WORDVEC_PARAMETER)) {
            try {
                wordVec_ = new WordVec(properties.getProperty(QA_WORDVEC_PARAMETER));
            } catch (IOException e) {}
        } else {
            wordVec_ = null;
        }
//        out = new BufferedWriter(new OutputStreamWriter(System.out));
    }

    private void readRelationWordMapping(String relInfoFile, String wordInfoFile, String relWordInfoFile) throws IOException {
        String line;

        Map<Integer, String> relIndexes = new HashMap<>();
        Map<Integer, String> wordIndexes = new HashMap<>();
        readDictFile(relInfoFile, relIndexes, pRel_);
        readDictFile(wordInfoFile, wordIndexes, pWord_);
        BufferedReader input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(relWordInfoFile))));
        int rowIndex = 0;
        while ((line = input.readLine()) != null) {
            int colIndex = 0;
            String currentRel = relIndexes.get(rowIndex);
            Map<String, Double> currentRelationDict;
            if (!pRelWord_.containsKey(currentRel)) {
                currentRelationDict = new HashMap<>();
                pRelWord_.put(currentRel, currentRelationDict);
            } else {
                currentRelationDict = pRelWord_.get(currentRel);
            }

            for (String val : line.split(" ")) {
                if (!val.equals("-inf")) {
                    currentRelationDict.put(wordIndexes.get(colIndex), Double.parseDouble(val));
                }
                ++colIndex;
            }
            ++rowIndex;
        }
        input.close();
    }

    private void readDictFile(String infoFile, Map<Integer, String> indexesDict, Map<String, Double> dict) throws IOException {
        String line;BufferedReader input = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(infoFile))));;
        while ((line = input.readLine()) != null) {
            String[] f = line.split(" ");
            try {
                int index = Integer.parseInt(f[1]);
                double score = Double.parseDouble(f[3]);
                dict.put(f[0], score);
                indexesDict.put(index, f[0]);
            } catch (Exception e) {
                System.err.println(line);
                e.printStackTrace();
            }
        }
        input.close();
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (isTraining_) {
            //long positivePredicates = document.getQaInstanceList().stream().filter(x -> predicates_.isEmpty() || predicates_.contains(x.getPredicate())).filter(Document.QaRelationInstance::getIsPositive).map(Document.QaRelationInstance::getPredicate).distinct().count();

            if (document.getQaInstanceCount() == 0
                    || document.getQaInstanceList().stream().filter(x -> predicates_.isEmpty() || predicates_.contains(x.getPredicate())).noneMatch(Document.QaRelationInstance::getIsPositive)) {
                return null;
            }
        }

        boolean isInTraining = ((document.getText().hashCode() & 0x7FFFFFFF) % 10) < 7;
        if (split_ && (isInTraining != isTraining_)) return null;

        DocumentWrapper documentWrapper = new DocumentWrapper(document);

//        int[] tokenMentions = new int[documentWrapper.getQuestionSentenceCount() < document.getSentenceCount()
//                ? document.getSentence(documentWrapper.getQuestionSentenceCount()).getFirstToken()
//                : document.getTokenCount()];
//        Arrays.fill(tokenMentions, -1);

        Set<String> questionFeatures = new HashSet<>();

        for (Document.Span span : document.getSpanList()) {
            if (span.hasEntityId() && span.getCandidateEntityScore(0) > Parameters.MIN_ENTITYID_SCORE) {
                int mentionIndex = 0;
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < documentWrapper.getQuestionSentenceCount()) {
                        String path = DependencyTreeUtils.getQuestionDependencyPath(document,
                                mention.getSentenceIndex(),
                                DependencyTreeUtils.getMentionHeadToken(document, mention));
                        if (path != null)
                            questionFeatures.add(path);
                        String template = NlpUtils.getQuestionTemplate(document, mention.getSentenceIndex(), span, mentionIndex).trim();
                        questionFeatures.add(template);
                        ++mentionIndex;
                    }
                }
            }
        }

        for (int sentence = 0; sentence < document.getSentenceCount() && sentence < documentWrapper.getQuestionSentenceCount(); ++sentence) {
            QuestionGraph qGraph = new QuestionGraph(documentWrapper, kb_, sentence, useFineTypes_);
            questionFeatures.addAll(qGraph.getEdgeFeatures());
        }



        if (wordVec_ != null) {
            // RVF FEatures
            int firstToken = document.getSentence(0).getFirstToken();
            int lastToken = document.getSentence(0).getLastToken();
            List<String> words = new ArrayList<>();
            for (int token = firstToken; token < lastToken; ++token) {
                if (Character.isAlphabetic(document.getToken(token).getPos().charAt(0)) &&
                        documentWrapper.getTokenSpan(token).isEmpty()) {
                    words.add(document.getToken(token).getText());
                }
            }
            embeddings.put(document.getSentence(0).getText(), wordVec_.getPhraseVec(words.toArray(new String[words.size()])));
        }



        Map<String, Integer> pQuesRelRank = null;
        Map<String, Double> pQuesRelScore = null;
        if (!pRelWord_.isEmpty()) {
            List<String> questionLemmas = documentWrapper.getQuestionLemmas(true);
            List<Pair<Double, String>> predicateScores = calculatePQuesRelScores(questionLemmas, document.getQaInstanceList());
            pQuesRelRank = new HashMap<>();
            pQuesRelScore = new HashMap<>();
            for (int i = 0; i < predicateScores.size(); ++i) {
                pQuesRelRank.put(predicateScores.get(i).second, predicateScores.size() - i);
                pQuesRelScore.put(predicateScores.get(i).second, predicateScores.get(i).first);
            }

        }

        PriorityQueue<Triple<Double, Document.QaRelationInstance, String>> scores = new PriorityQueue<>((o1, o2) -> o2.first.compareTo(o1.first));
        StringWriter strWriter = debug_ ? new StringWriter() : null;
        PrintWriter debugWriter = debug_ ? new PrintWriter(strWriter) : null;

        for (Document.QaRelationInstance instance : document.getQaInstanceList()) {
            if (!predicates_.isEmpty() && !predicates_.contains(instance.getPredicate())) {
                continue;
            }

            // Ignore self-triples and triples with numeric object (those are noisy)
            if (kb_.convertFreebaseMidRdf(instance.getObject()).equals(kb_.convertFreebaseMidRdf(instance.getSubject()))
                    || (!instance.getSubject().startsWith("http")
                        && instance.getSubject().contains("integer")  //|| instance.getSubject().contains("decimal"))
                        ))
                continue;

            if (isTraining_) {
                if (!instance.getIsPositive()) {
                    if (rnd_.nextInt(1000) > subsampleRate_) continue;
                }
            }

//            for (String str : features) {
//                alphabet_.put(str, (str.hashCode() & 0x7FFFFFFF) % alphabetSize_);
//            }
            List<String> answerFeatures = generateAnswerFeatures(instance);
            //generateFeatures(documentWrapper, instance, qDepPaths, features);
//            synchronized (out) {
//                out.write(instance.getIsPositive() ? "1" : "-1" + " |");
//                for (String feat : features) {
//                    out.write(" " + feat.replace(" ", "_").replace("\t", "_").replace("\n", "_").replace("|", "/"));
//                }
//                out.write("\n");
//            }

            if (pQuesRelRank != null) {
                int rank = pQuesRelRank.get(instance.getPredicate());
                if (rank == 1)
                    answerFeatures.add("RANK=1");
                else if (rank <= 2)
                    answerFeatures.add("RANK=2");
                else if (rank <= 3)
                    answerFeatures.add("RANK=3");
                else if (rank <= 5)
                    answerFeatures.add("RANK=4-5");
                else if (rank <= 10)
                    answerFeatures.add("RANK=6-10");
                else if (rank <= 50)
                    answerFeatures.add("RANK=11-50");
                else if (rank <= 100)
                    answerFeatures.add("RANK=51-100");
                else
                    answerFeatures.add("RANK=101-");
            }

            Set<Integer> feats = new HashSet<>();
            for (String aFeature : answerFeatures) {
                for (String qFeature : questionFeatures) {
                    String feature = qFeature + "||" + aFeature;
                    int id = (feature.hashCode() & 0x7FFFFFFF) % alphabetSize_;
                    if (debug_) {
                        debugWriter.println(id + "\t" + feature);
                    }
                    feats.add(id);
//                    feats.add(feature);
                }
            }

            if (isTraining_) {
                synchronized (dataset_) {
                    //System.out.println(instance.getIsPositive() + "\t" + features.stream().collect(Collectors.joining("\t")));
                    dataset_.add(feats, instance.getIsPositive());
                }
            } else {
                Triple<Double, Document.QaRelationInstance, String> tr =
                        new Triple<>(model_.probabilityOf(new BasicDatum<>(feats)).getCount(true), instance, "");

                // GOLD PREDICTIONS
                //tr.first = instance.getIsPositive() ? 1.0 : 0.0;
//                tr.first = pQuesRelScore.get(instance.getPredicate());

                if (debug_) {
                    model_.justificationOf(new BasicDatum<>(feats), debugWriter);
                    debugWriter.flush();
                    tr.third = strWriter.toString();
                    strWriter.getBuffer().setLength(0);
                }
                scores.add(tr);
            }
        }

        if (!isTraining_) {
            String[] fields = document.getText().split("\n");
            String utterance = fields[0];
            StringBuilder answers = new StringBuilder();
            answers.append("[");
            for (int i = 1; i < fields.length; ++i) {
                answers.append("\"");
                answers.append(fields[i].substring(1, fields[i].length() - 2));
                answers.append("\"");
                if (i < fields.length - 1) answers.append(",");
            }
            answers.append("]");

            StringBuilder prediction = new StringBuilder();
            prediction.append("[");
            Set<String> predictionsSet = new HashSet<>();

            StringBuilder debugInfo = debug_ ? new StringBuilder() : null;
            if (!scores.isEmpty()) {
                double bestScore = scores.peek().first;
                String bestSubject = scores.peek().second.getSubject();
                String bestPredicate = scores.peek().second.getPredicate();
                boolean shouldKeepAnswer;
                boolean first = true;
//                while (!scores.isEmpty() && (scores.peek().first == bestScore || scores.peek().first > 0.5)) {
                while (!scores.isEmpty()
                        && ((shouldKeepAnswer =
//                                (bestScore > 0.5 &&
//                                        && scores.peek().first > 0.5
                                        scores.peek().first >= bestScore)
//                                        && scores.peek().second.getSubject().equals(bestSubject))
//                                        && scores.peek().second.getPredicate().equals(bestPredicate)))
                            || debug_)) {

                    Triple<Double, Document.QaRelationInstance, String> tr = scores.poll();
                    Document.QaRelationInstance e = tr.second;

                    // Skip not extracted negative instances.
                    if (!shouldKeepAnswer && !e.getIsPositive()) continue;

                    String value;
                    if (e.getObject().startsWith("http://")) {
                        value = kb_.getEntityName(e.getObject());
                    } else {
                        value = e.getObject();
                        if (e.getObject().contains("^^")) {
                            value = e.getObject().split("\\^\\^")[0].replace("\"", "");
                            // Reformat dates
                            if (value.contains("-")) {
                                String[] parts = value.split("\\-");
                                String year = parts[0];
                                String month = parts[1].startsWith("0") ? parts[1].substring(1) : parts[1];
                                String day = parts.length == 3
                                        ? parts[2].startsWith("0") ? parts[2].substring(1) : parts[2]
                                        : "";
                                if (parts.length == 3)
                                    value = String.format("%s/%s/%s", month, day, year);
                                else if (parts.length == 2)
                                    value = String.format("%s/%s", month, year);
                            }
                        }
                    }
                    value = value.replace("\"", "\\\"").replace("\t", " ").replace("\n", " ");

                    if (shouldKeepAnswer && !predictionsSet.contains(value)) {
                        if (!first) prediction.append(",");
                        prediction.append("\"");
                        prediction.append(value);
                        prediction.append("\"");
                        first = false;
                        predictionsSet.add(value);
                    }

                    if (debug_) {
                        debugInfo.append("-------\n");
                        debugInfo.append("Correct: " + e.getIsPositive() + "\tExtracted: " + shouldKeepAnswer + "\t" + tr.first + "\t" + e.getSubject() + "\t" + e.getPredicate() + "\t" + e.getObject() + "\n");
                        debugInfo.append(tr.third);
                        debugInfo.append("\n\n");
                    }
                }
            }
            prediction.append("]");
            synchronized (this) {
                System.out.println(String.format("%s\t%s\t%s", utterance, answers, prediction));
                if (debug_) {
                    System.out.println(debugInfo + "-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-\n");
                }
            }
        }

        return document;
    }


    private List<Pair<Double, String>> calculatePQuesRelScores(List<String> questionLemmas,
                                                        List<Document.QaRelationInstance> relations) {
        return relations.stream()
                .map(Document.QaRelationInstance::getPredicate)
                .distinct()
                .map(x -> new Pair<>(calcPQuesRelScore(questionLemmas, x), x))
                .sorted()
                .collect(Collectors.toList());
    }

    private double calcPQuesRelScore(List<String> questionLemmas, String predicate) {
        double res = 0.0;
        int pos = -1;
        if ((pos = predicate.indexOf(".cvt.")) != -1) {
            predicate = predicate.substring(pos + 5);
        }

        if (pRel_.containsKey(predicate)) {
            Map<String, Double> currentRelWordMap = pRelWord_.get(predicate);
            res = pRel_.get(predicate);

            for (String lemma : questionLemmas) {
                if (currentRelWordMap.containsKey(lemma)) {

                    res += currentRelWordMap.get(lemma);
                } else {
                    res += -10; //Math.log(1.0 / pWord_.size());
                }
            }
        } else {
            int counter = 0;
            for (String piece : predicate.split("\\.")) {
                if (pRel_.containsKey(piece)) {
                    ++counter;

                    res += pRel_.get(piece);
                    Map<String, Double> currentRelWordMap = pRelWord_.get(piece);
                    for (String lemma : questionLemmas) {
                        if (currentRelWordMap.containsKey(lemma)) {
                            res += currentRelWordMap.get(lemma);
                        } else {
                            res += -10; //Math.log(1.0 / pWord_.size());
                        }
                    }
                } else {
                    res += -10 * (questionLemmas.size() + 1);
                }
            }
            if (counter > 0) {
                res /= counter;
            }
        }

        return res == 0 ? Double.NEGATIVE_INFINITY : res;
    }

    private List<String> generateAnswerFeatures(Document.QaRelationInstance instance) {
        List<String> res = new ArrayList<>();
        res.add("PREDICATE=" + instance.getPredicate());

        if (partialPredicateNames_) {
            int pos = -1;
            while ((pos = instance.getPredicate().indexOf(".", pos + 1)) != -1) {
                res.add("PREDICATE_PART=" + instance.getPredicate().substring(0, pos));
            }
        }

        if (useFineTypes_) {
            res.addAll(kb_.getEntityTypes(instance.getObject(), false).stream()
                    .map(x -> x.contains("/") ? x.substring(x.lastIndexOf("/") + 1) : x)
                    .filter(x -> !x.startsWith("common.")
                            && !x.startsWith("base.")
                            && !x.startsWith("user."))
                    .map(x -> "TYPE=" + x)
                    .collect(Collectors.toSet()));
            StmtIterator iter = kb_.getSubjectPredicateTriples(instance.getObject(), "people.person.gender");
            while (iter.hasNext()) {
                res.add("GENDER=" + kb_.getEntityName(iter.nextStatement().getObject().asResource().getLocalName()));
            }
        }
        return res;
    }

    @Override
    public void finishProcessing() {
        if (model_ == null) {

            for (String question : embeddings.keySet()) {
                float[] currentQ = embeddings.get(question);
                System.out.println("\n\n>>>>>>> " + question);
                for (Pair<String, Double> closest :
                        embeddings.entrySet().stream().filter(e -> !e.getKey().equals(question))
                                .map(e -> {
                                    double dist = 0;
                                    for (int i = 0; i < currentQ.length; ++i) {
                                        dist += (currentQ[i] - e.getValue()[i]) * (currentQ[i] - e.getValue()[i]);
                                    }
                                    return new Pair<>(e.getKey(), Math.sqrt(dist));
                                })
                                .sorted((e1, e2) -> e1.second.compareTo(e2.second))
                                .limit(10)
                                .collect(Collectors.toList())) {
                    System.out.println(closest.second + "\t" + closest.first);
                }
            }

            dataset_.summaryStatistics();
//            dataset_.selectFeaturesBinaryInformationGain(10000);
            //dataset_.applyFeatureMaxCountThreshold(dataset_.size() / 10000);
//            dataset_.applyFeatureCountThreshold(2);
//            dataset_.summaryStatistics();

            // TODO(dsavenk): Comment this out for now.
            if (!datasetFile_.equals("None")) {
                try {
                    PrintWriter out = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(datasetFile_))));
                    for (Datum<Boolean, Integer> d : dataset_) {
                        out.println((d.label() ? "1" : "-1") + "\t" + d.asFeatures().stream().sorted().map(Object::toString).collect(Collectors.joining("\t")));
                    }
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            LinearClassifierFactory<Boolean, Integer> classifierFactory_ = new LinearClassifierFactory<>(1e-4, false, regularizer_);
            classifierFactory_.useQuasiNewton(true);
            //classifierFactory_.setTuneSigmaHeldOut();
            //classifierFactory_.setRetrainFromScratchAfterSigmaTuning(true);
            //classifierFactory_.setHeldOutSearcher(new GoldenSectionLineSearch(0.01, 0.01, 10.0, true));
//            classifierFactory_.useInPlaceStochasticGradientDescent(50, 1000, regularizer_);
//            classifierFactory_.setMinimizerCreator(() -> new SGDMinimizer(regularizer_, 50, -1, 1000));

            classifierFactory_.setVerbose(true);
            model_ = classifierFactory_.trainClassifier(dataset_);
            LinearClassifier.writeClassifier(model_, modelFile_);
        }
    }

    private String getFeature(String[] parts, String... valParts) {
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < parts.length; ++i) {
            res.append(parts[i]);
            res.append(":");
            res.append(valParts[i]);
            res.append("|");
        }
        return res.toString();
    }


    static class QuestionGraph {
        enum NodeType {
            REGULAR,
            QWORD,
            QFOCUS,
            QTOPIC,
            QVERB,
            PREPOSITION
        }

        static class Node {
            public List<Pair<Integer, String>> parent = new ArrayList<>();
            public NodeType type = NodeType.REGULAR;
            public List<String> values = new ArrayList<>();

            public String[] getValues() {
                String[] res = new String[values.size()];
                int i = 0;
                for (String value : values) {
                    res[i++] = type != NodeType.REGULAR && type != NodeType.PREPOSITION ? type + "=" + value : value;
                }
                return res;
            }

            @Override
            public String toString() {
                return type != NodeType.REGULAR && type != NodeType.PREPOSITION ? type + "=" + values : values.toString();
            }
        }

        private List<Node> nodes_ = new ArrayList<>();

        QuestionGraph(DocumentWrapper document, KnowledgeBase kb, int sentence, boolean useFreebaseTypes) {
            int firstToken = document.document().getSentence(sentence).getFirstToken();
            int lastToken = document.document().getSentence(sentence).getLastToken();
            int[] tokenToNodeIndex = new int[lastToken - firstToken];
            Arrays.fill(tokenToNodeIndex, -1);
            for (int token = firstToken; token < lastToken; ++token) {
                int mentionHead = document.getTokenMentionHead(token);
                Node node = null;
                if (Character.isAlphabetic(document.document().getToken(token).getPos().charAt(0))) {
                    if (mentionHead != -1) {
                        if (mentionHead == token) {
                            node = new Node();
                            boolean measure = document.isTokenMeasure(token);
                            node.type = measure ? NodeType.REGULAR : NodeType.QTOPIC;
                            if (false && !measure && useFreebaseTypes) {
                                for (Document.Span span : document.getTokenSpan(token)) {
                                    for (int i = 0; i < span.getCandidateEntityIdCount() && span.getCandidateEntityScore(i) >= Parameters.MIN_ENTITYID_SCORE; ++i) {
                                        node.values.addAll(kb.getEntityTypes(span.getCandidateEntityId(i), false).stream()
                                                .map(x -> x.contains("/") ? x.substring(x.lastIndexOf("/") + 1) : x)
                                                .filter(x -> !x.startsWith("common.") && !x.startsWith("base.") && !x.startsWith("user."))
                                                .collect(Collectors.toSet()));
                                    }
                                }
                            } else {
                                node.values = document.document().getToken(token).getNer().equals("O")
                                        ? Arrays.asList(document.document().getToken(token).getLemma())
                                        : Arrays.asList(document.document().getToken(token).getNer());
                            }
                        }
                    } else {
                        if (!document.document().getToken(token).getPos().startsWith("D")
                                && !document.document().getToken(token).getPos().startsWith("PD")) {
                            node = new Node();
                            node.values = Arrays.asList(document.document().getToken(token).getLemma());
                            if (document.document().getToken(token).getPos().startsWith("W")) {
                                node.type = NodeType.QWORD;
                            } else if (document.document().getToken(token).getPos().startsWith("V") || document.document().getToken(token).getPos().startsWith("MD")) {
                                node.type = NodeType.QVERB;
                            } else {
                                if (document.document().getToken(token).getPos().startsWith("IN")) {
                                    node.type = NodeType.PREPOSITION;
                                } else {
                                    node.type = NodeType.REGULAR;
                                }
                            }
                        }
                    }
                }
                if (node != null) {
                    tokenToNodeIndex[token - firstToken] = nodes_.size();
                    nodes_.add(node);
                }
            }

            for (int token = firstToken; token < lastToken; ++token) {
                if (tokenToNodeIndex[token - firstToken] != -1) {
                    Node node = nodes_.get(tokenToNodeIndex[token - firstToken]);
                    int parent = document.document().getToken(token).getDependencyGovernor();
                    if (parent != 0) {
                        --parent;
                        if (document.document().getToken(firstToken + parent).getPos().startsWith("W") && node.type == NodeType.REGULAR) {
                            node.type = NodeType.QFOCUS;
                        }
                        if (tokenToNodeIndex[parent] != -1) {
                            node.parent.add(new Pair<>(tokenToNodeIndex[parent], document.document().getToken(token).getDependencyType()));
                        }
                    }
                }
            }
        }

        List<String> getEdgeFeatures() {
            List<String> res = new ArrayList<>();
            Node qwordNode = null;
            Node qfocusNode = null;
            Node qverbNode = null;
            Node qtargetNode = null;
            for (Node node : nodes_) {
                if (node != null) {
                    switch (node.type) {
                        case QWORD:
                            qwordNode = node;
                            break;
                        case QFOCUS:
                            qfocusNode = node;
                            break;
                        case QVERB:
                            qverbNode = node;
                            break;
                        case QTOPIC:
                            qtargetNode = node;
                            break;
                    }
                }
                if (node != null) {
                    if (node.type != NodeType.PREPOSITION) {
                        for (String feat : node.getValues()) {
                            res.add(feat);
                        }
                    }

                    for (Pair<Integer, String> parent : node.parent) {
                        for (String nodeStr : node.getValues()) {
                            for (String parentNodeStr : nodes_.get(parent.first).getValues()) {
                                res.add(nodeStr + "->" + parentNodeStr);
                                res.add(nodeStr + "-" + parent.second + "->" + parentNodeStr);
                            }
                        }
                    }
                }
            }

            for (String qword : qwordNode != null ? qwordNode.getValues() : new String[] {""}) {
                for (String qfocus : qfocusNode != null ? qfocusNode.getValues() : new String[] {""}) {
                    for (String qverb : qverbNode != null ? qverbNode.getValues() : new String[] {""}) {
                        for (String qtarget : qtargetNode != null ? qtargetNode.getValues() : new String[] {""}) {
                            res.add(qword + " + " + qfocus + " + " + qverb + " + " + qtarget);
                        }
                    }
                }
            }

            return res;
        }

        @Override
        public String toString() {
            StringBuilder res = new StringBuilder();
            for (Node node : nodes_) {
                if (node != null) {
                    res.append(node.type);
                    res.append("\t");
                    res.append(node.values.toString());
                    res.append("\n");
                }
            }
            return res.toString();
        }
    }
}

