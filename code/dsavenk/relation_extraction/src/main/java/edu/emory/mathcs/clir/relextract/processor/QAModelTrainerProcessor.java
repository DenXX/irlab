package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.util.*;
import java.util.function.BiFunction;

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
    private Map<String, Map<String, Set<String>>> sop = Collections.synchronizedMap(new HashMap<>());

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
        DocumentWrapper doc = new DocumentWrapper(document);
        int questionSentencesCount = doc.getQuestionSentenceCount();

        Set<Integer> questionSpans = new HashSet<>();
        Set<Integer> answerSpans = new HashSet<>();

        processSpanMentions(doc, (mention, spanIndex) -> {
            if (mention.getSentenceIndex() < questionSentencesCount) {
                questionSpans.add(spanIndex);
            }
            return null;
        });
        if (!questionSpans.isEmpty()) {
            processSpanMentions(doc, (mention, spanIndex) -> {
                if (mention.getSentenceIndex() >= questionSentencesCount
                        && !questionSpans.contains(spanIndex)) {
                    answerSpans.add(spanIndex);
                }
                return null;
            });

            if (!answerSpans.isEmpty()) {
                for (int questionSpan : questionSpans) {
                    for (int answerSpan : answerSpans) {
                        Set<String> relations = getSpansRelations(doc, questionSpan, answerSpan);
                    }
                }
                return document;
            }
        }

        return null;
    }

    private Set<String> getSpansRelations(DocumentWrapper doc, int questionSpan, int answerSpan) {
        Set<String> res = new HashSet<>();
        Document.Span qSpan = doc.document().getSpan(questionSpan);
        Document.Span aSpan = doc.document().getSpan(answerSpan);
        if (qSpan.hasEntityId()) {
            cacheTopicTriples(qSpan);
        }
        if (aSpan.hasEntityId()) {
            cacheTopicTriples(aSpan);
        }

        return res;
    }

    private void cacheTopicTriples(Document.Span span) {
        for (int i = 0; i < span.getCandidateEntityIdCount()
                && span.getCandidateEntityScore(i) >= Parameters.MIN_ENTITYID_SCORE; ++i) {
            System.out.println(span.getCandidateEntityId(i));
//            if (!sop.containsKey(qSpan.getCandidateEntityId(i))) {
//                Map<String, Set<String>> op = new HashMap<>();
//                List<KnowledgeBase.Triple> triples = kb_.getSubjectTriplesCvt(qSpan.getCandidateEntityId(i));
//                for (KnowledgeBase.Triple t : triples) {
//                    if (!op.containsKey(t.object))
//                        op.put(t.object, new HashSet<>());
//                    op.get(t.object).add(t.predicate);
//                }
//                sop.put(qSpan.getCandidateEntityId(i), op);
//            }
        }
    }

    private void processSpanMentions(DocumentWrapper doc, BiFunction<Document.Mention, Integer, Void> func) {
        int spanIndex = 0;
        for (Document.Span span : doc.document().getSpanList()) {
            if ("MEASURE".equals(span.getType()) ||
                    (span.hasEntityId() && span.getCandidateEntityScore(0) >= Parameters.MIN_ENTITYID_SCORE)) {
                for (Document.Mention mention : span.getMentionList()) {
                    func.apply(mention, spanIndex);
                }
            }
            ++spanIndex;
        }
    }
}
