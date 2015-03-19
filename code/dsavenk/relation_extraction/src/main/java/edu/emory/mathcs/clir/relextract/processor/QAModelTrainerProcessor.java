package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.rdf.model.Literal;
import com.hp.hpl.jena.rdf.model.Statement;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.util.*;

/**
 * Created by dsavenk on 3/18/15.
 */
public class QAModelTrainerProcessor extends Processor {

    private static class QAPair {
        Document.NlpDocument document;
        int questionSentence;
        int answerSpan;
        String relation;
        boolean correct;

        QAPair(Document.NlpDocument doc, int qSentence, int aSpan, String rel, boolean isCorrect) {
            document = doc;
            questionSentence = qSentence;
            answerSpan = aSpan;
            relation = rel;
            correct = isCorrect;
        }
    }

    private final KnowledgeBase kb_;
    private Map<String, Map<String, Set<Statement>>> sop = Collections.synchronizedMap(new HashMap<>());

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
        Document.NlpDocument.Builder docBuilder = null;
        DocumentWrapper doc = new DocumentWrapper(document);
        int questionSentencesCount = doc.getQuestionSentenceCount();

        Set<String> questionSpanIds = new HashSet<>();
        Set<String> answerSpanIds = new HashSet<>();

        for (Document.Span span : document.getSpanList()) {
            Set<String> entityIds = new HashSet<>();
            if ("MEASURE".equals(span.getType())) {
                entityIds.add(span.getValue());
            } else {
                for (int i = 0; i < span.getCandidateEntityIdCount()
                        && span.getCandidateEntityScore(i) >= Parameters.MIN_ENTITYID_SCORE; ++i) {
                    entityIds.add(span.getCandidateEntityId(i));
                }
            }

            if (!entityIds.isEmpty()) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < questionSentencesCount
                            && !"MEASURE".equals(span.getType())) {
                        questionSpanIds.addAll(entityIds);
                        break;
                    } else {
                        answerSpanIds.addAll(entityIds);
                    }
                }
            }
        }
        answerSpanIds.removeAll(questionSpanIds);
        if (!answerSpanIds.isEmpty() && !questionSpanIds.isEmpty()) {
            questionSpanIds.stream().forEach(this::cacheTopicTriples);

//            docBuilder = document.toBuilder();
//
//            for (String id : questionSpanIds) {
//                for (String relatedId : sop.get(id).keySet()) {
//                    for (Statement st : sop.get(id).get(relatedId)) {
//                        docBuilder.addQaInstanceBuilder()
//                                .setIsPositive(answerSpanIds.contains(relatedId))
//                                .setSubject(id)
//                                .setPredicate(st.getPredicate().getLocalName())
//                                .setObject(st.getObject().asNode().toString(null, true));
//                    }
//                }
//            }
//            return docBuilder.build();
        }

        return null;
    }

    private void cacheTopicTriples(String id) {
        System.out.println(id);
        if (false && !sop.containsKey(id)) {
            Map<String, Set<Statement>> op = new HashMap<>();
            List<Statement> triples = kb_.getSubjectTriplesCvt(id);
            for (Statement st : triples) {
                if (st.getObject().isResource()) {
                    if (st.getObject().asResource().getLocalName().startsWith("m.")) {
                        String objectMid = "/" + st.getObject().asResource().getLocalName().replace(".", "/");
                        if (!op.containsKey(objectMid))
                            op.put(objectMid, new HashSet<>());
                        op.get(objectMid).add(st);
                    }
                } else {
                    String lang = st.getObject().asLiteral().getLanguage();
                    RDFDatatype type = st.getObject().asLiteral().getDatatype();
                    if (type != null) {
                        String val = getStringRepresentation(st.getObject().asLiteral());
                        if (!op.containsKey(val)) op.put(val, new HashSet<>());
                        op.get(val).add(st);
                    }
                }
            }
            sop.put(id, op);
        }
    }

    private String getStringRepresentation(Literal literal) {
        RDFDatatype type = literal.getDatatype();
        if (type == null) {
            return literal.getString();
        } else {
            return literal.getString();
        }
    }
}
