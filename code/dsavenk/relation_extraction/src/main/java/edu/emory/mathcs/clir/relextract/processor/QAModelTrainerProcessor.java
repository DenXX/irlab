package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.rdf.model.StmtIterator;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import edu.emory.mathcs.clir.representations.WordVec;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.classify.RVFDataset;
import edu.stanford.nlp.classify.WeightedDataset;
import edu.stanford.nlp.ling.BasicDatum;
import edu.stanford.nlp.ling.Datum;
import edu.stanford.nlp.ling.RVFDatum;
import edu.stanford.nlp.optimization.SGDMinimizer;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.*;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dsavenk on 3/20/15.
 */
public class QAModelTrainerProcessor extends Processor {

    private final WeightedDataset<String, Integer> dataset_ = new WeightedDataset<>();
    private final RVFDataset<Boolean, String> simDataset_ = new RVFDataset<>();
    private final KnowledgeBase kb_;
    private WordVec wordVec_ = null;
    private Map<String, float[]> embeddings = new HashMap<>();

    private Random rnd_ = new Random(42);
    private String modelFile_;
    private LinearClassifier<String, Integer> model_ = null;
    private LinearClassifier<Boolean, String> simModel_ = null;
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
    private boolean tuneSigma_ = false;

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
    public static final String QA_TUNESIGMA_PARAMETER = "qa_tunesigma";

    BufferedWriter out;

    // Similarity based QA.
    public static final String QA_INDEX_PATH_PARAMETER = "qa_index";
    private static final double SCORE_THRESHOLD = 0.8;
    private IndexSearcher searcher_;
    private QueryBuilder queryBuilder_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public QAModelTrainerProcessor(Properties properties) throws IOException {
        super(properties);
        kb_ = KnowledgeBase.getInstance(properties);
        modelFile_ = properties.getProperty(QA_MODEL_PARAMETER);
        if (properties.containsKey(QA_TEST_PARAMETER)) {
            //model_ = LinearClassifier.readClassifier(modelFile_);
            simModel_ = LinearClassifier.readClassifier(modelFile_);
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
        if (properties.containsKey(QA_TUNESIGMA_PARAMETER)) {
            tuneSigma_ = true;
        }
//        out = new BufferedWriter(new OutputStreamWriter(System.out));

        searcher_ = new IndexSearcher(
                DirectoryReader.open(
                        FSDirectory.open(new File(properties.getProperty(QA_INDEX_PATH_PARAMETER)))));
        //searcher_.setSimilarity(new BM25Similarity());

        Map<String, Analyzer> analyzers = new HashMap<>();
        analyzers.put(BuildSearchIndexProcessor.RELATIONS_FIELD_NAME, new KeywordAnalyzer());
        analyzers.put(BuildSearchIndexProcessor.QUESTION_FIELD_NAME, new EnglishAnalyzer(CharArraySet.EMPTY_SET));
        analyzers.put(BuildSearchIndexProcessor.QUESTION_TEMPLATE_FIELD_NAME, new SimpleAnalyzer());
        analyzers.put(BuildSearchIndexProcessor.QUESTION_FEATURES_FIELD_NAME, new KeywordAnalyzer());
        queryBuilder_ = new QueryBuilder(new PerFieldAnalyzerWrapper(new SimpleAnalyzer(), analyzers));
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

        Map<String, Double> entityScores = new HashMap<>();

        Set<String> questionFeatures = new HashSet<>();
        String bestEntity = "";
        Pair<Double, Double> bestEntityScore = new Pair<>(0.0, 0.0);
        int answersCount = 0;
        for (Document.Span span : document.getSpanList()) {
            if (span.hasEntityId() && span.getCandidateEntityScore(0) > Parameters.MIN_ENTITYID_SCORE) {
                int mentionIndex = 0;
                boolean answerEntity = false;
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < documentWrapper.getQuestionSentenceCount()) {

                        for (int i = 0; i < span.getCandidateEntityIdCount(); ++i) {
                            entityScores.put(span.getCandidateEntityId(i), span.getCandidateEntityScore(i));
                            Pair<Double, Double> currentScore = new Pair<>(span.getCandidateEntityScore(i), (double)mention.getText().split("\\s+").length); // span.getCandidateEntityScore(i);
                            if (currentScore.first > bestEntityScore.first || (currentScore.first.equals(bestEntityScore.first) && currentScore.second > bestEntityScore.second))
                            {
                                bestEntity = span.getCandidateEntityId(i);
                                bestEntityScore = currentScore;
                            }
                        }

                        String path = DependencyTreeUtils.getQuestionDependencyPath(document,
                                mention.getSentenceIndex(),
                                DependencyTreeUtils.getMentionHeadToken(document, mention));
                        if (path != null)
                            questionFeatures.add(path);
                        String template = NlpUtils.getQuestionTemplate(document, mention.getSentenceIndex(), span, mentionIndex).trim();
                        questionFeatures.add(template);
                        ++mentionIndex;
                    } else {
                        answerEntity = true;
                    }
                }
                if (answerEntity) ++answersCount;
            }
        }

        final int finalAnswersCount = answersCount;

        for (int sentence = 0; sentence < document.getSentenceCount() && sentence < documentWrapper.getQuestionSentenceCount(); ++sentence) {
            QuestionGraph qGraph = new QuestionGraph(documentWrapper, kb_, sentence, useFineTypes_);
            questionFeatures.addAll(qGraph.getEdgeFeatures());
        }

//        Map<String, Integer> pQuesRelRank = null;
//        Map<String, Double> pQuesRelScore = null;
//        if (!pRelWord_.isEmpty()) {
//            List<String> questionLemmas = documentWrapper.getQuestionLemmas(true);
//            List<Pair<Double, String>> predicateScores = calculatePQuesRelScores(questionLemmas, document.getQaInstanceList());
//            pQuesRelRank = new HashMap<>();
//            pQuesRelScore = new HashMap<>();
//            for (int i = 0; i < predicateScores.size(); ++i) {
//                pQuesRelRank.put(predicateScores.get(i).second, predicateScores.size() - i);
//                pQuesRelScore.put(predicateScores.get(i).second, predicateScores.get(i).first);
//            }
//
//        }

        // SIMILARITY BASED QA
        Set<String> templates = new HashSet<>();
        for (Document.Span span : document.getSpanList()) {
            int index = 0;
            for (Document.Mention mention : span.getMentionList()) {
                if (mention.getSentenceIndex() < documentWrapper.getQuestionSentenceCount()) {
                    String template = NlpUtils.getQuestionTemplate(document, mention.getSentenceIndex(), span, index);
                    templates.add(template);
                    break;
                }
                ++index;
            }
        }

        Map<String, List<RVFDatum<Boolean, String>>> relationDatums = new HashMap<>();
        for (String template : templates) {
            Query q = queryBuilder_.createBooleanQuery(BuildSearchIndexProcessor.QUESTION_TEMPLATE_FIELD_NAME, template);
            TopDocs docs = searcher_.search(q, 20);

            for (int i = 0; i < docs.scoreDocs.length; ++i) {
                final org.apache.lucene.document.Document doc = searcher_.doc(docs.scoreDocs[i].doc);

                Set<String> matches = new HashSet<>();
                int queryLength = 1;
                if (q instanceof BooleanQuery) {
                    queryLength = ((BooleanQuery) q).getClauses().length;
                    for (BooleanClause subquery : ((BooleanQuery) q).getClauses()) {
                        if (searcher_.explain(subquery.getQuery(), docs.scoreDocs[i].doc).isMatch()) {
                            matches.add(((TermQuery) subquery.getQuery()).getTerm().text());
                        }
                    }
                } else if (q instanceof TermQuery) {
                    if (searcher_.explain(q, docs.scoreDocs[i].doc).isMatch()) {
                        matches.add(((TermQuery)q).getTerm().text());
                    }
                }

                Set<String> posMatches = new HashSet<>();
                for (int questionSentence = 0; questionSentence < documentWrapper.getQuestionSentenceCount(); ++questionSentence) {
                    for (Document.Token token : document.getTokenList()) {
                        if (matches.contains(token.getLemma())) posMatches.add(token.getPos());
                    }
                }

                Counter<String> baseFeats = new ClassicCounter<>();
                baseFeats.setCount("SCORE", docs.scoreDocs[i].score);
                baseFeats.setCount("PERC_MAX_SCORE", docs.scoreDocs[i].score / docs.getMaxScore());
                baseFeats.setCount("RANK", i);
                baseFeats.setCount("RANK_1", i == 0 ? 1.0 : 0.0);
                baseFeats.setCount("RANK_5", i < 5 ? 1.0 : 0.0);
                baseFeats.setCount("RANK_10", i < 10 ? 1.0 : 0.0);
                baseFeats.setCount("RANK_20", i < 20 ? 1.0 : 0.0);
                baseFeats.setCount("RANK_50", i < 50 ? 1.0 : 0.0);
                baseFeats.setCount("MATCH_PERC", 1.0 * matches.size() / queryLength);
                for (String match : matches) {
                    baseFeats.setCount("MATCH(" + match + ")", 1.0);
                }
                for (String posMatch : posMatches) {
                    baseFeats.setCount("MATCH_POS(" + posMatch + ")", 1.0);
                }


                Stream.of(doc.getValues(BuildSearchIndexProcessor.RELATIONS_FIELD_NAME))
                        .collect(Collectors.groupingBy(Function.<String>identity(), Collectors.counting()))
                        .forEach((predicate, count) -> {
                            RVFDatum<Boolean, String> instance = new RVFDatum<>(baseFeats);
                            instance.setLabel(false);
                            Counter<String> feats = instance.asFeaturesCounter();
                            try {
                                feats.setCount("DOC_FREQUENCY", Math.log(1 + searcher_.getIndexReader().docFreq(new Term(BuildSearchIndexProcessor.RELATIONS_FIELD_NAME, predicate))));
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            feats.setCount("ANSWER_MATCHES_PERCENT", 1.0 * count / (finalAnswersCount == 0 ? 1 : finalAnswersCount));
                            feats.setCount("PREDICATE(" + predicate + ")", 1.0);
                            relationDatums.putIfAbsent(predicate, new ArrayList<>());
                            relationDatums.get(predicate).add(instance);
                        });

            }
        }
//        relationScores.entrySet().stream()
//                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
//                .limit(10)
//                .forEach(x -> System.out.println(x.getKey() + "\t" + x.getValue()));
        // SIMILARITY BASED QA

        PriorityQueue<Triple<Double, Document.QaRelationInstance, String>> scores = new PriorityQueue<>((o1, o2) -> {
            int res = o2.first.compareTo(o1.first);
            if (res != 0) return res;
            return Double.compare(entityScores.get(o2.second.getSubject()), entityScores.get(o1.second.getSubject()));
        });
        StringWriter strWriter = debug_ ? new StringWriter() : null;
        PrintWriter debugWriter = debug_ ? new PrintWriter(strWriter) : null;

        if (isTraining_) {
            //addTrainingInstances(document, questionFeatures, finalAnswersCount);
            addTrainingInstancesSim(document, finalAnswersCount, relationDatums);
        } else {
            //predictAnswer(document, questionFeatures, scores);
            predictAnswerSim(document, relationDatums, scores);

        }

//        for (Document.QaRelationInstance instance : document.getQaInstanceList()) {
//            if (!predicates_.isEmpty() && !predicates_.contains(instance.getPredicate())) {
//                continue;
//            }
//
//            // Ignore self-triples and triples with numeric object (those are noisy)
//            if (kb_.convertFreebaseMidRdf(instance.getObject()).equals(kb_.convertFreebaseMidRdf(instance.getSubject()))
//                    || (!instance.getSubject().startsWith("http")
//                        && instance.getSubject().contains("integer")  //|| instance.getSubject().contains("decimal"))
//                        ))
//                continue;
//
//            if (isTraining_) {
//                if (!instance.getIsPositive()) {
//                    if (rnd_.nextInt(1000) > subsampleRate_) continue;
//                }
//            }
//
////            for (String str : features) {
////                alphabet_.put(str, (str.hashCode() & 0x7FFFFFFF) % alphabetSize_);
////            }
//            List<String> answerFeatures = generateAnswerFeatures(instance);
//            //generateFeatures(documentWrapper, instance, qDepPaths, features);
////            synchronized (out) {
////                out.write(instance.getIsPositive() ? "1" : "-1" + " |");
////                for (String feat : features) {
////                    out.write(" " + feat.replace(" ", "_").replace("\t", "_").replace("\n", "_").replace("|", "/"));
////                }
////                out.write("\n");
////            }
//
//            if (pQuesRelRank != null) {
//                int rank = pQuesRelRank.get(instance.getPredicate());
//                if (rank == 1)
//                    answerFeatures.add("RANK=1");
//                else if (rank <= 2)
//                    answerFeatures.add("RANK=2");
//                else if (rank <= 3)
//                    answerFeatures.add("RANK=3");
//                else if (rank <= 5)
//                    answerFeatures.add("RANK=4-5");
//                else if (rank <= 10)
//                    answerFeatures.add("RANK=6-10");
//                else if (rank <= 50)
//                    answerFeatures.add("RANK=11-50");
//                else if (rank <= 100)
//                    answerFeatures.add("RANK=51-100");
//                else
//                    answerFeatures.add("RANK=101-");
//            }
//
//            Set<Integer> feats = new HashSet<>();
//            for (String aFeature : answerFeatures) {
//                for (String qFeature : questionFeatures) {
//                    String feature = qFeature + "||" + aFeature;
//                    int id = (feature.hashCode() & 0x7FFFFFFF) % alphabetSize_;
//                    if (debug_) {
//                        debugWriter.println(id + "\t" + feature);
//                    }
//                    feats.add(id);
////                    feats.add(feature);
//                }
//            }
//
//            if (isTraining_) {
//                synchronized (dataset_) {
//                    //System.out.println(instance.getIsPositive() + "\t" + features.stream().collect(Collectors.joining("\t")));
//                    //dataset_.add(feats, instance.getIsPositive());
//                    dataset_.add(feats, instance.getIsPositive());
//                }
//            } else {
//                Triple<Double, Document.QaRelationInstance, String> tr =
//                        new Triple<>(model_.probabilityOf(new BasicDatum<>(feats)).getCount(true), instance, "");
//
//                // GOLD PREDICTIONS
//                //tr.first = instance.getIsPositive() ? 1.0 : 0.0;
////                tr.first = pQuesRelScore.get(instance.getPredicate());
//
//                if (debug_) {
//                    model_.justificationOf(new BasicDatum<>(feats), debugWriter);
//                    debugWriter.flush();
//                    tr.third = strWriter.toString();
//                    strWriter.getBuffer().setLength(0);
//                }
//                scores.add(tr);
//            }
//        }

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
                                        scores.peek().first >= bestScore
                                        && scores.peek().second.getSubject().equals(bestSubject))
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

    private void predictAnswerSim(Document.NlpDocument document, Map<String, List<RVFDatum<Boolean, String>>> relationDatums, PriorityQueue<Triple<Double, Document.QaRelationInstance, String>> scores) {
       scores.addAll(document.getQaInstanceList().stream()
                .map(entry -> {
                    //float weight = 1.0f / Math.max(1, (entry.getPredicateObjectsCount() - entry.getValue().size() + finalAnswersCount));
                    //if (Float.isFinite(weight) && !Float.isNaN(weight)) {
                    double curScore = 0;
                    int count = relationDatums.getOrDefault(entry.getPredicate(), Collections.emptyList()).size();
                    for (RVFDatum<Boolean, String> instance : relationDatums.getOrDefault(entry.getPredicate(), Collections.emptyList())) {
                        instance.asFeaturesCounter().setCount("PREDICATE_OBJECTS", entry.getPredicateObjectsCount());
                        curScore = Math.max(curScore, simModel_.scoreOf(instance, true));
                    }
                    return new Triple<>(curScore, entry, "");
                })
                .collect(Collectors.toList()));
    }

    private void predictAnswer(Document.NlpDocument document, Set<String> questionFeatures, PriorityQueue<Triple<Double, Document.QaRelationInstance, String>> scores) {
        Datum<String, Integer> e = new BasicDatum<>(questionFeatures.stream().map(f -> (f.hashCode() & 0x7FFFFFFF) % alphabetSize_).collect(Collectors.toList()));
        Set<String> labels = new HashSet<>(model_.labels());

        String bestPredicate = document.getQaInstanceList().stream()
                .map(Document.QaRelationInstance::getPredicate)
                .filter(labels::contains)
                .map(x -> new Pair<>(x, model_.scoreOf(e, x)))
                .max((e1, e2) -> e1.second.compareTo(e2.second))
                .map(Pair::first)
                .orElse("");

        scores.addAll(document.getQaInstanceList()
                .stream()
                .filter(x -> x.getPredicate().equals(bestPredicate))
                .map(x -> new Triple<>(1.0, x, ""))
                .collect(Collectors.toList()));
    }

    private void addTrainingInstances(Document.NlpDocument document, Set<String> questionFeatures, int finalAnswersCount) {
        document.getQaInstanceList().stream()
                .filter(Document.QaRelationInstance::getIsPositive)
                .collect(Collectors.groupingBy(instance -> new Pair<>(instance.getSubject(), instance.getPredicate())))
                .entrySet()
                .stream()
                .forEach(entry -> {
                    float weight = 1.0f * entry.getValue().size() / Math.max(1, (entry.getValue().get(0).getPredicateObjectsCount() - entry.getValue().size() + finalAnswersCount));
                    if (Float.isFinite(weight) && !Float.isNaN(weight)) {
                        synchronized (dataset_) {
                            dataset_.add(questionFeatures.stream().map(f -> (f.hashCode() & 0x7FFFFFFF) % alphabetSize_).collect(Collectors.toList()), entry.getKey().second, weight);
                        }
                    }
                });
    }

    private void addTrainingInstancesSim(Document.NlpDocument document,
                                         int finalAnswersCount,
                                         Map<String, List<RVFDatum<Boolean, String>>> relationDatums) {
        
        final float[] maxWeight = new float[1];
        document.getQaInstanceList().stream()
                .collect(Collectors.groupingBy(instance -> new Pair<>(instance.getSubject(), instance.getPredicate())))
                .entrySet()
                .stream()
                .forEach(e1 -> {
                     long positive = e1.getValue().stream().filter(Document.QaRelationInstance::getIsPositive).count(); 
                     float weight = 1.0f * positive / Math.max(1, (e1.getValue().get(0).getPredicateObjectsCount() - positive + finalAnswersCount));
                     maxWeight[0] = Math.max(maxWeight[0], weight);
                });
 
        
        document.getQaInstanceList().stream()
                .collect(Collectors.groupingBy(instance -> new Pair<>(instance.getSubject(), instance.getPredicate())))
                .entrySet()
                .stream()
                .forEach(entry -> {
                        for (RVFDatum<Boolean, String> instance : relationDatums.getOrDefault(entry.getKey().second, Collections.emptyList())) {
                            long positive = entry.getValue().stream().filter(Document.QaRelationInstance::getIsPositive).count(); 
                            float weight = 1.0f * positive / Math.max(1, (entry.getValue().get(0).getPredicateObjectsCount() - positive + finalAnswersCount));
                     
                            instance.setLabel(entry.getValue().get(0).getIsPositive() && weight == maxWeight[0]);
                            instance.asFeaturesCounter().setCount("PREDICATE_OBJECTS", entry.getValue().get(0).getPredicateObjectsCount());
                            Counter<String> feats = instance.asFeaturesCounter();
                            //for (String feat : feats.keySet()) {
                            //    feats.setCount(feat, feats.getCount(feat) * weight);
                            //}
                            synchronized (simDataset_) {
                                simDataset_.add(instance);
                            }
                        }
                });
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
        // REGULAR QA
        trainQaSimModel();
    }

    private void trainQaSimModel() {
        if (simModel_ == null) {
            simDataset_.summaryStatistics();
//            dataset_.selectFeaturesBinaryInformationGain(10000);
            //dataset_.applyFeatureMaxCountThreshold(dataset_.size() / 10000);
            simDataset_.applyFeatureCountThreshold(2);
            simDataset_.summaryStatistics();

            LinearClassifierFactory<Boolean, String> classifierFactory_ = new LinearClassifierFactory<>(1e-4, false, regularizer_);
            classifierFactory_.useQuasiNewton(true);
            if (tuneSigma_) {
                classifierFactory_.setTuneSigmaHeldOut();
                classifierFactory_.setRetrainFromScratchAfterSigmaTuning(true);
            }
            //classifierFactory_.setHeldOutSearcher(new GoldenSectionLineSearch(0.01, 0.01, 10.0, true));

            // We are doing this to specify batch size, otherwise we can get array index out of bounds for large datasets.
            //classifierFactory_.setMinimizerCreator(() -> new SGDMinimizer(regularizer_, 50, -1, 1000));

            classifierFactory_.setVerbose(true);
            simModel_= classifierFactory_.trainClassifier(simDataset_);
            LinearClassifier.writeClassifier(simModel_, modelFile_);
        }
    }

    private void trainQaModel() {
        if (model_ == null) {
            dataset_.summaryStatistics();
//            dataset_.selectFeaturesBinaryInformationGain(10000);
            //dataset_.applyFeatureMaxCountThreshold(dataset_.size() / 10000);
            dataset_.applyFeatureCountThreshold(2);
            dataset_.summaryStatistics();

            // TODO(dsavenk): Comment this out for now.
            if (!datasetFile_.equals("None")) {
                try {
                    PrintWriter out = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(datasetFile_))));
                    for (Datum<String, Integer> d : dataset_) {
                        //out.println((d.label() ? "1" : "-1") + "\t" + d.asFeatures().stream().sorted().map(Object::toString).collect(Collectors.joining("\t")));
                        out.println(d.label() + "\t" + d.asFeatures().stream().sorted().map(Object::toString).collect(Collectors.joining("\t")));
                    }
                    out.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            LinearClassifierFactory<String, Integer> classifierFactory_ = new LinearClassifierFactory<>(1e-4, false, regularizer_);
            //classifierFactory_.useQuasiNewton(true);
            if (tuneSigma_) {
                classifierFactory_.setTuneSigmaHeldOut();
                classifierFactory_.setRetrainFromScratchAfterSigmaTuning(true);
            }
            //classifierFactory_.setHeldOutSearcher(new GoldenSectionLineSearch(0.01, 0.01, 10.0, true));
            //classifierFactory_.useInPlaceStochasticGradientDescent(50, 1000, regularizer_);
            // We are doing this to specify batch size, otherwise we can get array index out of bounds for large datasets.
            classifierFactory_.setMinimizerCreator(() -> new SGDMinimizer(regularizer_, 50, -1, 1000));

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

