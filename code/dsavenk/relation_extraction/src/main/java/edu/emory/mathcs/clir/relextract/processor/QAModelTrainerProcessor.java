package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.classify.Dataset;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.classify.LinearClassifierFactory;
import edu.stanford.nlp.optimization.QNMinimizer;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 3/20/15.
 */
public class QAModelTrainerProcessor extends Processor {

    private final Dataset<Boolean, String> dataset_ = new Dataset<>();
    private final KnowledgeBase kb_;
    private Random rnd_ = new Random(42);
    private String modelFile;

    private int alphabetSize = 10000000;
    private Map<String, Integer> alphabet_ = Collections.synchronizedMap(new HashMap<>());
    private Set<String> predicates_ = new HashSet<>();

    public static final String QA_MODEL_PARAMETER = "qa_model_path";
    public static final String QA_PREDICATES_PARAMETER = "qa_predicates";

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
        modelFile = properties.getProperty(QA_MODEL_PARAMETER);
        try {
            BufferedReader input = new BufferedReader(new FileReader(properties.getProperty(QA_PREDICATES_PARAMETER)));
            String line;
            while ((line = input.readLine()) != null) {
                predicates_.add(line);
            }
            input.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (document.getQaInstanceCount() == 0
                || document.getQaInstanceList().stream().filter(x -> predicates_.contains(x.getPredicate())).noneMatch(Document.QaRelationInstance::getIsPositive)) {
            return null;
        }

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
                        qDepPaths.add(DependencyTreeUtils.getQuestionDependencyPath(document,
                                mention.getSentenceIndex(),
                                DependencyTreeUtils.getMentionHeadToken(document, mention)));
                    }
                }
            }
        }

        Set<String> features = new HashSet<>();
        for (Document.QaRelationInstance instance : document.getQaInstanceList()) {
            if (!predicates_.contains(instance.getPredicate())) {
                continue;
            }

            if (instance.getIsPositive()) {
                if (rnd_.nextInt(100) > 30) continue;
            } else {
                if (rnd_.nextInt(1000) > 10) continue;
            }

//            for (String str : features) {
//                alphabet_.put(str, (str.hashCode() & 0x7FFFFFFF) % alphabetSize);
//            }
            features.clear();
            generateFeatures(documentWrapper, instance, qDepPaths, features);
            synchronized (dataset_) {
                //System.out.println(instance.getIsPositive() + "\t" + features.stream().collect(Collectors.joining("\t")));
                //dataset_.add(features.stream().map(x -> (x.hashCode() & 0x7FFFFFFF) % alphabetSize).collect(Collectors.toSet()), instance.getIsPositive());
                dataset_.add(features, instance.getIsPositive());
            }
        }

        return document;
    }

    @Override
    public void finishProcessing() {
        dataset_.summaryStatistics();
        LinearClassifierFactory<Boolean, String> classifierFactory_ =
                new LinearClassifierFactory<>(1e-4, false, 0.0);
        //classifierFactory_.setTuneSigmaHeldOut();
        classifierFactory_.setMinimizerCreator(() -> {
            QNMinimizer min = new QNMinimizer(15);
            min.useOWLQN(true, 1.0);
            return min;
        });
        classifierFactory_.setVerbose(true);

        LinearClassifier<Boolean, String> model_ = classifierFactory_.trainClassifier(dataset_);
        model_.saveToFilename(modelFile);
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
        List<String> answerEntityTypes = Collections.emptyList();
        // TODO(denxx): Include answer entity types and some other info
//            kb_.getEntityTypes(instance.getObject(), false)
//                .stream()
//                .map(x -> x.contains("/") ? x.substring(x.lastIndexOf("/") + 1) : x)
//                .filter(x -> x.contains("common.topic"))
//                .collect(Collectors.toList());

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
            res.append("|");
            res.append(valParts[i]);
        }
        return res.toString();
    }
}

