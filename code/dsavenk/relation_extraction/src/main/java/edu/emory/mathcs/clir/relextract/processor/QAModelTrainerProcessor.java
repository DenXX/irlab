package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import com.hp.hpl.jena.sparql.util.NodeFactoryExtra;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.utils.DocumentUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.classify.Dataset;
import org.apache.xerces.impl.dv.XSSimpleType;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 3/20/15.
 */
public class QAModelTrainerProcessor extends Processor {

    private final Dataset<Boolean, String> dataset_ = new Dataset<>();
    private final KnowledgeBase kb_;

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
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (document.getQaInstanceCount() == 0) {
            return null;
        }

        Set<String> features = new HashSet<>();
        for (Document.QaRelationInstance instance : document.getQaInstanceList()) {
            features.clear();
            generateFeatures(new DocumentWrapper(document), instance, features);
            synchronized (dataset_) {
                dataset_.add(features, instance.getIsPositive());
            }
        }

        return document;
    }

    @Override
    public void finishProcessing() {

    }

    private void generateFeatures(DocumentWrapper document,
                                  Document.QaRelationInstance instance,
                                  Set<String> features) {
        List<String> questionEntityTypes = kb_.getEntityTypes(instance.getSubject(), false)
                .stream()
                .map(x -> x.contains("/") ? x.substring(x.lastIndexOf("/") + 1) : x)
                .filter(x -> x.contains("common.topic"))
                .collect(Collectors.toList());
        List<String> answerEntityTypes = kb_.getEntityTypes(instance.getObject(), false)
                .stream()
                .map(x -> x.contains("/") ? x.substring(x.lastIndexOf("/") + 1) : x)
                .filter(x -> x.contains("common.topic"))
                .collect(Collectors.toList());

        Set<String> qWords = document.getQuestionWords();
        Set<String> qVerbs = document.getQuestionVerbs();
        Set<String> qFocuses = document.getQuestionFocus();

        for (Document.Span span : document.document().getSpanList()) {
//            for ()
        }

        for (String qWord : qWords) {
            features.add(String.format("qword=%s|arelation=%s", qWord, instance.getPredicate()));
            for (String answerEntityType : answerEntityTypes) {
                features.add(String.format("qword=%s|atopic=%s", qWord, answerEntityType));
                features.add(String.format("qword=%s|atopic=%s|arelation=%s", qWord, answerEntityType, instance.getPredicate()));
            }
        }

        for (String qVerb : qVerbs) {
            features.add(String.format("qverb=%s|arelation=%s", qVerb, instance.getPredicate()));
            for (String answerEntityType : answerEntityTypes) {
                features.add(String.format("qverb=%s|atopic=%s", qVerb, answerEntityType));
                features.add(String.format("qverb=%s|atopic=%s|arelation=%s", qVerb, answerEntityType, instance.getPredicate()));
            }
        }

        for (String qFocus : qFocuses) {
            features.add(String.format("qfocus=%s|arelation=%s", qFocus, instance.getPredicate()));
            for (String answerEntityType : answerEntityTypes) {
                features.add(String.format("qfocus=%s|atopic=%s", qFocus, answerEntityType));
                features.add(String.format("qfocus=%s|atopic=%s|arelation=%s", qFocus, answerEntityType, instance.getPredicate()));
            }
        }

        for (String questionEntityType : questionEntityTypes) {
            features.add(String.format("qtopic=%s|arelation=%s", questionEntityType, instance.getPredicate()));
            for (String answerEntityType : answerEntityTypes) {
                features.add(String.format("qtopic=%s|atopic=%s", questionEntityType, answerEntityType));
                features.add(String.format("qtopic=%s|atopic=%s|arelation=%s", questionEntityType, answerEntityType, instance.getPredicate()));
            }
        }

        for (String qWord : qWords) {
            for (String qVerb : qVerbs) {
                for (String qFocus : qFocuses) {
                    for (String questionEntityType : questionEntityTypes) {
                        features.add(String.format("qword=%s|qverb=%s|qfocus=%s|qtopic=%s|arelation=%s", qWord, qVerb, qFocus, questionEntityType, instance.getPredicate()));
                        for (String answerEntityType : answerEntityTypes) {
                            features.add(String.format("qword=%s|qverb=%s|qfocus=%s|qtopic=%s|atopic=%s", qWord, qVerb, qFocus, questionEntityType, answerEntityType));
                            features.add(String.format("qword=%s|qverb=%s|qfocus=%s|qtopic=%s|atopic=%s|arelation=%s", qWord, qVerb, qFocus, questionEntityType, answerEntityType, instance.getPredicate()));
                        }
                    }
                }
            }
        }
    }
}

