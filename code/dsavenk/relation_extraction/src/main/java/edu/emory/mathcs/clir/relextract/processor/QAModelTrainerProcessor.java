package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.util.Pair;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dsavenk on 3/20/15.
 */
public class QAModelTrainerProcessor extends Processor {

    private final Dataset<Boolean, Integer> dataset_ = new Dataset<>();
    private final KnowledgeBase kb_;
    private Random rnd_ = new Random(42);
    private String modelFile_;
    private LinearClassifier<Boolean, Integer> model_ = null;
    private String datasetFile_;
    private boolean split_ = false;

    private int alphabetSize_ = 10000000;
    private Map<String, Integer> alphabet_ = Collections.synchronizedMap(new HashMap<>());
    private Set<String> predicates_ = new HashSet<>();
    private double subsampleRate_ = 10;

    public static final String QA_MODEL_PARAMETER = "qa_model_path";
    public static final String QA_DATASET_PARAMETER = "qa_dataset_path";
    public static final String QA_PREDICATES_PARAMETER = "qa_predicates";
    public static final String QA_TEST_PARAMETER = "qa_test";
    public static final String QA_SUBSAMPLE_PARAMETER = "qa_subsample";
    public static final String SPLIT_DATASET_PARAMETER = "qa_split_data";



    BufferedWriter out;

    String[] feat1 = {"qword", "arelation"};
    String[] feat2 = {"qword", "atopic"};
    String[] feat3 = {"qword", "atopic", "arelation"};
    String[] feat4 = {"qverb", "arelation"};
    String[] feat5 = {"qverb", "atopic"};
    String[] feat6 = {"qverb", "atopic", "arelation"};
    String[] feat7 = {"qfocus", "arelation"};
    String[] feat8 = {"qfocus", "atopic"};
    String[] feat9 = {"qfocus", "atopic", "arelation"};
    String[] feat10 = {"qtopic", "arelation"};
    String[] feat11 = {"qtopic", "atopic"};
    String[] feat12 = {"qtopic", "atopic", "arelation"};
    String[] feat13 = {"qword", "qverb", "qfocus", "qtopic", "arelation"};
    String[] feat14 = {"qword", "qverb", "qfocus", "qtopic", "atopic"};
    String[] feat15 = {"qword", "qverb", "qfocus", "qtopic", "atopic", "arelation"};
    String[] feat16 = {"qdeppath", "arelation"};

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
        }
        if (properties.containsKey(SPLIT_DATASET_PARAMETER)) {
            split_ = true;
        }
        if (properties.containsKey(QA_SUBSAMPLE_PARAMETER)) {
            subsampleRate_ = Double.parseDouble(properties.getProperty(QA_SUBSAMPLE_PARAMETER));
        }
        datasetFile_ = properties.getProperty(QA_DATASET_PARAMETER);
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
//        out = new BufferedWriter(new OutputStreamWriter(System.out));
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        boolean isTraining = model_ == null;

        if (isTraining) {
            if (document.getQaInstanceCount() == 0
                    || document.getQaInstanceList().stream().filter(x -> predicates_.isEmpty() || predicates_.contains(x.getPredicate())).noneMatch(Document.QaRelationInstance::getIsPositive)) {
                return null;
            }
        }

        boolean isInTraining = ((document.getText().hashCode() & 0x7FFFFFFF) % 10) < 7;
        if (split_ && (isInTraining != isTraining)) return null;

        DocumentWrapper documentWrapper = new DocumentWrapper(document);

//        int[] tokenMentions = new int[documentWrapper.getQuestionSentenceCount() < document.getSentenceCount()
//                ? document.getSentence(documentWrapper.getQuestionSentenceCount()).getFirstToken()
//                : document.getTokenCount()];
//        Arrays.fill(tokenMentions, -1);

        Set<String> qDepPaths = new HashSet<>();

        for (Document.Span span : document.getSpanList()) {
            if (span.hasEntityId() && span.getCandidateEntityScore(0) > Parameters.MIN_ENTITYID_SCORE) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < documentWrapper.getQuestionSentenceCount()) {
                        String path = DependencyTreeUtils.getQuestionDependencyPath(document,
                                mention.getSentenceIndex(),
                                DependencyTreeUtils.getMentionHeadToken(document, mention));
                        if (path != null)
                            qDepPaths.add(path);
                    }
                }
            }
        }

        List<String> questionFeatures = new ArrayList<>();
        for (int sentence = 0; sentence < document.getSentenceCount() && sentence < documentWrapper.getQuestionSentenceCount(); ++sentence) {
            questionFeatures.addAll(new QuestionGraph(documentWrapper, sentence).getEdgeFeatures());
        }

        PriorityQueue<Pair<Double, Document.QaRelationInstance>> scores = new PriorityQueue<>((o1, o2) -> o2.first.compareTo(o1.first));
        for (Document.QaRelationInstance instance : document.getQaInstanceList()) {
            if (!predicates_.isEmpty() && !predicates_.contains(instance.getPredicate())) {
                continue;
            }

            // Ignore self-triples
            if (kb_.convertFreebaseMidRdf(instance.getObject()).equals(kb_.convertFreebaseMidRdf(instance.getSubject())))
                continue;

            if (isTraining) {
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
            Set<Integer> feats = new HashSet<>();
            for (String qFeature : questionFeatures) {
                for (String aFeature : answerFeatures) {
                    feats.add(((qFeature + "||" + aFeature).hashCode() & 0x7FFFFFFF) % alphabetSize_);
                }
            }

            // TODO(dsavenk): Move this to question features
            for (String qFeature : qDepPaths) {
                for (String aFeature : answerFeatures) {
                    feats.add(((qFeature + "||" + aFeature).hashCode() & 0x7FFFFFFF) % alphabetSize_);
                }
            }

            if (isTraining) {
                synchronized (dataset_) {
                    //System.out.println(instance.getIsPositive() + "\t" + features.stream().collect(Collectors.joining("\t")));
                    dataset_.add(feats, instance.getIsPositive());
                    //dataset_.add(features, instance.getIsPositive());
                }
            } else {
                scores.add(new Pair<>(model_.probabilityOf(new BasicDatum<>(feats)).getCount(true), instance));
            }
        }

        if (!isTraining) {
            String[] fields = document.getText().split("\n");
            String utterance = fields[0];
            StringBuilder answers = new StringBuilder();
            answers.append("[");
            for (int i = 1; i < fields.length; ++i) {
                answers.append(fields[i].replace(",", ""));
                if (i < fields.length - 1) answers.append(",");
            }
            answers.append("]");

            StringBuilder prediction = new StringBuilder();
            prediction.append("[");
            Set<String> predictionsSet = new HashSet<>();
            if (!scores.isEmpty()) {
                double bestScore = scores.peek().first;
                boolean first = true;
                while (!scores.isEmpty() && scores.peek().first == bestScore) {
                    Document.QaRelationInstance e = scores.poll().second;
                    if (!first) prediction.append(",");
                    prediction.append("\"");
                    if (e.getObject().startsWith("http://")) {
                        prediction.append(kb_.getEntityName(e.getObject()));
                    } else {
                        String value = e.getObject();
                        if (e.getObject().contains("^^")) {
                            value = e.getObject().split("\\^\\^")[0].replace("\"", "");
                            // Reformat dates
                            if (value.contains("-")) {
                                String[] parts = value.split("\\-");
                                if (parts.length == 3)
                                    value = String.format("%s/%s/%s", parts[1], parts[2], parts[0]);
                                else if (parts.length == 2)
                                    value = String.format("%s/%s", parts[1], parts[0]);
                            }
                        }
                        if (!predictionsSet.contains(value)) {
                            prediction.append(value.replace("\"", "\\\""));
                            predictionsSet.add(value);
                        }
                    }
                    prediction.append("\"");
                    first = false;
                }
            }
            prediction.append("]");
            synchronized (this) {
                System.out.println(String.format("%s\t%s\t%s", utterance, answers, prediction));
            }
        }

        return document;
    }

    private List<String> generateAnswerFeatures(Document.QaRelationInstance instance) {
        return Arrays.asList(instance.getPredicate());
    }

    @Override
    public void finishProcessing() {
        if (model_ == null) {
            dataset_.summaryStatistics();

            // TODO(dsavenk): Comment this out for now.
//            try {
//                PrintWriter out = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(datasetFile_))));
//                for (Datum<Boolean, Integer> d : dataset_) {
//                    out.println(d.label() ? "1" : "-1" + " | " + d.asFeatures().stream().sorted().map(Object::toString).collect(Collectors.joining(" ")));
//                }
//                out.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }

            LinearClassifierFactory<Boolean, Integer> classifierFactory_ =
                    new LinearClassifierFactory<>(1e-4, false, 0.0);
            //classifierFactory_.setTuneSigmaHeldOut();
            classifierFactory_.useInPlaceStochasticGradientDescent();

//        classifierFactory_.setMinimizerCreator(() -> {
//            QNMinimizer min = new QNMinimizer(15);
//            min.useOWLQN(true, 1.0);
//            return min;
//        });
            classifierFactory_.setVerbose(true);
            model_ = classifierFactory_.trainClassifier(dataset_);
            LinearClassifier.writeClassifier(model_, modelFile_);
        }
    }

    private void generateFeatures(DocumentWrapper document,
                                  Document.QaRelationInstance instance,
                                  Set<String> qDepPaths,
                                  Set<String> features) {
        List<String> questionEntityTypes = kb_.getEntityTypes(instance.getSubject(), false)
                .stream()
                .map(x -> x.contains("/") ? x.substring(x.lastIndexOf("/") + 1) : x)
                .filter(x -> !x.contains("common.topic"))
                .collect(Collectors.toList());
        List<String> answerEntityTypes =
            kb_.getEntityTypes(instance.getObject(), false)
                .stream()
                .map(x -> x.contains("/") ? x.substring(x.lastIndexOf("/") + 1) : x)
                .filter(x -> !x.contains("common.topic"))
                .collect(Collectors.toList());

        Set<String> qWords = document.getQuestionWords();
        Set<String> qVerbs = document.getQuestionVerbs();
        Set<String> qFocuses = document.getQuestionFocus();

        for (String qWord : qWords) {
            features.add(getFeature(feat1, qWord, instance.getPredicate()));
            for (String answerEntityType : answerEntityTypes) {
                features.add(getFeature(feat2, qWord, answerEntityType));
                features.add(getFeature(feat3, qWord, answerEntityType, instance.getPredicate()));
            }
        }

        for (String qVerb : qVerbs) {
            features.add(getFeature(feat4, qVerb, instance.getPredicate()));
            for (String answerEntityType : answerEntityTypes) {
                features.add(getFeature(feat5, qVerb, answerEntityType));
                features.add(getFeature(feat6, qVerb, answerEntityType, instance.getPredicate()));
            }
        }

        for (String qFocus : qFocuses) {
            features.add(getFeature(feat7, qFocus, instance.getPredicate()));
            for (String answerEntityType : answerEntityTypes) {
                features.add(getFeature(feat8, qFocus, answerEntityType));
                features.add(getFeature(feat9, qFocus, answerEntityType, instance.getPredicate()));
            }
        }

        for (String questionEntityType : questionEntityTypes) {
            features.add(getFeature(feat10, questionEntityType, instance.getPredicate()));
            for (String answerEntityType : answerEntityTypes) {
                features.add(getFeature(feat11, questionEntityType, answerEntityType));
                features.add(getFeature(feat12, questionEntityType, answerEntityType, instance.getPredicate()));
            }
        }

        for (String qWord : qWords) {
            for (String qVerb : qVerbs) {
                for (String qFocus : qFocuses) {
                    for (String questionEntityType : questionEntityTypes) {
                        features.add(getFeature(feat13, qWord, qVerb, qFocus, questionEntityType, instance.getPredicate()));
                        for (String answerEntityType : answerEntityTypes) {
                            features.add(getFeature(feat14, qWord, qVerb, qFocus, questionEntityType, answerEntityType));
                            features.add(getFeature(feat15, qWord, qVerb, qFocus, questionEntityType, answerEntityType, instance.getPredicate()));
                        }
                    }
                }
            }
        }

        for (String depPath : qDepPaths) {
            for (String questionEntityType : questionEntityTypes) {
                features.add(getFeature(feat16, depPath, questionEntityType, instance.getPredicate()));
            }
        }

//        Set<String> edges = new HashSet<>();
//        for (int sentence = 0; sentence < document.getQuestionSentenceCount(); ++sentence) {
//            for (int token = document.document().getSentence(sentence).getFirstToken();
//                 token < document.document().getSentence(sentence).getLastToken(); ++token) {
//                if (tokenMentions[token] == -1 || tokenMentions[token] == token) {
//                    StringBuilder edge = new StringBuilder();
//                    edge.append(document.document().getToken(token).getDependencyType());
//                    edge.append("(");
//                    int gov = document.document().getToken(token).getDependencyGovernor();
//                    if (gov > 0) {
//                        gov = document.document().getSentence(sentence).getFirstToken() + gov - 1;
//                    } else {
//
//                    }
//                }
//            }
//        }
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
            public String value = "";

            @Override
            public String toString() {
                return type != NodeType.REGULAR && type != NodeType.PREPOSITION ? type + "=" + value : value;
            }
        }

        private List<Node> nodes_ = new ArrayList<>();

        QuestionGraph(DocumentWrapper document, int sentence) {
            int firstToken = document.document().getSentence(sentence).getFirstToken();
            int[] tokenToNodeIndex = new int[document.document().getSentence(sentence).getLastToken() - firstToken];
            Arrays.fill(tokenToNodeIndex, -1);
            for (int token = firstToken;
                    token < document.document().getSentence(sentence).getLastToken();
                    ++token) {

                int mentionHead = document.getTokenMentionHead(token);
                Node node = null;
                if (Character.isAlphabetic(document.document().getToken(token).getPos().charAt(0))) {
                    if (mentionHead != -1) {
                        if (mentionHead == token) {
                            node = new Node();
                            node.type = NodeType.QTOPIC;
                            node.value = document.document().getToken(token).getNer().equals("O")
                                    ? document.document().getToken(token).getLemma()
                                    : document.document().getToken(token).getNer();
                        }
                    } else {
                        if (!document.document().getToken(token).getPos().startsWith("D")
                                && !document.document().getToken(token).getPos().startsWith("PD")) {
                            node = new Node();
                            node.value = document.document().getToken(token).getLemma();
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

            for (int token = firstToken;
                 token < document.document().getSentence(sentence).getLastToken();
                 ++token) {
                if (tokenToNodeIndex[token] != -1) {
                    Node node = nodes_.get(tokenToNodeIndex[token]);
                    int parent = document.document().getToken(token).getDependencyGovernor();
                    if (parent != 0) {
                        --parent;
                        if (document.document().getToken(firstToken + parent).getPos().startsWith("W")) {
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
            for (Node node : nodes_) {
                if (node != null) {
                    String nodeStr = node.toString();
                    if (node.type != NodeType.PREPOSITION) {
                        res.add(nodeStr);
                    }
                    for (Pair<Integer, String> parent : node.parent) {
                        String parentNode = nodes_.get(parent.first).toString();
                        res.add(nodeStr + "->" + parentNode);
                        res.add(nodeStr + "-" + parent.second + "->" + parentNode);
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
                    res.append(node.value);
                    res.append("\n");
                }
            }
            return res.toString();
        }
    }
}

