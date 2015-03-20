package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;
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
public class QAExamplesBuilderProcessor extends Processor {

    private final KnowledgeBase kb_;
    private Map<String, Map<String, Set<Statement>>> sop = Collections.synchronizedMap(new HashMap<>());
    private Set<RDFDatatype> dateTypes_ = new HashSet<>();

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public QAExamplesBuilderProcessor(Properties properties) {
        super(properties);
        kb_ = KnowledgeBase.getInstance(properties);

        dateTypes_.add(XSDDatatype.XSDdate);
        dateTypes_.add(XSDDatatype.XSDgYear);
        dateTypes_.add(XSDDatatype.XSDgYearMonth);
        dateTypes_.add(XSDDatatype.XSDgMonthDay);
        dateTypes_.add(XSDDatatype.XSDgMonth);
        dateTypes_.add(XSDDatatype.XSDgDay);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        Document.NlpDocument.Builder docBuilder = null;
        DocumentWrapper doc = new DocumentWrapper(document);
        int questionSentencesCount = doc.getQuestionSentenceCount();

        Set<String> questionSpanIds = new HashSet<>();
        Set<String> answerSpanIds = new HashSet<>();
        Set<String> answerDates = new HashSet<>();

        for (Document.Span span : document.getSpanList()) {
            Set<String> entityIds = new HashSet<>();
            if ("MEASURE".equals(span.getType())) {
                if (span.getNerType().equals("DATE")) {
                    boolean skip = false;
                    int pos1 = span.getValue().indexOf("value=");
                    if (pos1 == -1) {
                        pos1 = span.getValue().indexOf("altVal=");
                    }
                    if (pos1 != -1) {
                        pos1 = span.getValue().indexOf("\"", pos1);
                        int pos2 = span.getValue().indexOf("\"", pos1 + 2);
                        if (pos2 != -1) {
                            String value = span.getValue().substring(pos1 + 1, pos2);
                            for (int i = 0; i < value.length(); ++i) {
                                if (!Character.isDigit(value.charAt(i)) &&
                                        value.charAt(i) != '-' && value.charAt(i) != 'X') {
                                    skip = true;
                                }
                            }
                            if (!skip) {
                                answerDates.add(value);
                            }
                        } else {
                            answerDates.add(span.getValue());
                        }
                    } else {
                        answerDates.add(span.getValue());
                    }
                } else {
                    entityIds.add(span.getValue());
                }
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

            docBuilder = document.toBuilder();

            for (String id : questionSpanIds) {
                for (String relatedId : sop.get(id).keySet()) {
                    for (Statement st : sop.get(id).get(relatedId)) {
                        Document.QaRelationInstance.Builder qaInstance = docBuilder.addQaInstanceBuilder()
                                .setSubject(id)
                                .setPredicate(st.getPredicate().getLocalName())
                                .setObject(st.getObject().asNode().toString(null, true));

                        if (st.getObject().isLiteral() && dateTypes_.contains(st.getObject().asLiteral().getDatatype())) {
                            for (String date : answerDates) {
                                if (date.matches("[X0-9]{4}(-[X0-9]{2}){0,2}")) {
                                    String year = date.substring(0, 4);
                                    String month = date.length() > 4 ?
                                            date.substring(5, 7)
                                            : "XX";
                                    String day = date.length() > 7
                                            ? date.substring(8)
                                            : "XX";
                                    qaInstance.setIsPositive(kb_.matchDatesSoft(year, month, day, (XSDDateTime)st.getObject().asLiteral().getValue()));
                                } else {
                                    qaInstance.setIsPositive(answerSpanIds.contains(relatedId));
                                }
                            }
                        } else {
                            qaInstance.setIsPositive(answerSpanIds.contains(relatedId));
                        }
                    }
                }
            }
            return docBuilder.build();
        }

        return null;
    }

    private void cacheTopicTriples(String id) {
        if (!sop.containsKey(id)) {
            Map<String, Set<Statement>> op = new HashMap<>();
            List<Statement> triples = kb_.getSubjectTriplesCvt(id); // Collections.emptyList();
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