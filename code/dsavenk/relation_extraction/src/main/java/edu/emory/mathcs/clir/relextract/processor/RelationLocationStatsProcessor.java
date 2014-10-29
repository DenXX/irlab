package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.util.*;

/**
 * Created by dsavenk on 10/15/14.
 */
public class RelationLocationStatsProcessor extends Processor {
    private int questionOnly = 0;
    private Map<String, Integer> questionOnlyPred = new HashMap<>();
    private int answerOnly = 0;
    private Map<String, Integer> answerOnlyPred = new HashMap<>();
    private int questionAnswerOnly = 0;
    private Map<String, Integer> questionAnswerOnlyPred = new HashMap<>();
    private int total = 0;
    private Map<String, Integer> totalPred = new HashMap<>();

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public RelationLocationStatsProcessor(Properties properties) {
        super(properties);
        KnowledgeBase.getInstance(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (document.getRelationCount() == 0) return null;

        int questionLengthChars = document.getQuestionLength();
        int questionLengthTokens = 0;

        for (Document.Token token : document.getTokenList()) {
            if (token.getBeginCharOffset() >= questionLengthChars) break;
            ++questionLengthTokens;
        }

        // For each document we save relations that occur in each part of the
        // QnA pair.
        Set<KnowledgeBase.Triple> questionRelations = new HashSet<>();
        Set<KnowledgeBase.Triple> questionAnswerRelations = new HashSet<>();
        Set<KnowledgeBase.Triple> answerRelations = new HashSet<>();
        Set<KnowledgeBase.Triple> totalRelations = new HashSet<>();
        for (Document.Relation rel : document.getRelationList()) {
//                if (!rel.getRelation().equals("people.person.date_of_birth")) continue;
            boolean inQuestion = false;
            boolean inAnswer = false;
            boolean inQuestionAnswer = false;
            for (Document.Mention mention1 : document.getSpan(
                    rel.getSubjectSpan()).getMentionList()) {
                boolean subjInQuestion = mention1.getTokenBeginOffset() <
                        questionLengthTokens;
                for (Document.Mention mention2 : document.getSpan(
                        rel.getObjectSpan()).getMentionList()) {
                    boolean objInQuestion = mention2.getTokenBeginOffset() <
                            questionLengthTokens;
                    if (subjInQuestion) {
                        if (objInQuestion) {
                            if (mention1.getSentenceIndex()
                                    == mention2.getSentenceIndex()) {
                                inQuestion = true;
                            }
                        } else {
                            inQuestionAnswer = true;
                        }
                    } else if (objInQuestion) {
                        inQuestionAnswer = true;
                    } else if (mention1.getSentenceIndex() ==
                            mention2.getSentenceIndex()) {
                        inAnswer = true;
                    }
                }
            }
            KnowledgeBase.Triple triple = new KnowledgeBase.Triple(
                    document.getSpan(rel.getSubjectSpan()).getEntityId(),
                    rel.getRelation(),
                    document.getSpan(rel.getObjectSpan()).hasEntityId()
                            ? document.getSpan(rel.getObjectSpan()).getEntityId()
                            :document.getSpan(rel.getObjectSpan()).getValue());

            totalRelations.add(triple);
            if (inQuestion && !inAnswer && !inQuestionAnswer) {
                questionRelations.add(triple);
            } else if (inAnswer && !inQuestion && !inQuestionAnswer) {
                answerRelations.add(triple);
            } else if (inQuestionAnswer && !inQuestion && !inAnswer) {
                questionAnswerRelations.add(triple);
            }
        }

        boolean has = false;
        boolean first = true;
        for (KnowledgeBase.Triple triple : questionRelations) {
            if (!totalPred.containsKey(triple.predicate)) {
                totalPred.put(triple.predicate, 0);
                questionOnlyPred.put(triple.predicate, 0);
                answerOnlyPred.put(triple.predicate, 0);
                questionAnswerOnlyPred.put(triple.predicate, 0);
            }

            totalPred.put(triple.predicate, totalPred.get(triple.predicate) + 1);
            if (!answerRelations.contains(triple) &&
                    !questionAnswerRelations.contains(triple)) {
                if (first) {
                    System.out.println(">>> Question only");
                    first = false;
                }
                has = true;
                System.out.println(triple);
                ++questionOnly;
                questionOnlyPred.put(triple.predicate, questionOnlyPred.get(triple.predicate) + 1);
            }
        }
        first = true;
        for (KnowledgeBase.Triple triple : answerRelations) {
            if (!totalPred.containsKey(triple.predicate)) {
                totalPred.put(triple.predicate, 0);
                questionOnlyPred.put(triple.predicate, 0);
                answerOnlyPred.put(triple.predicate, 0);
                questionAnswerOnlyPred.put(triple.predicate, 0);
            }
            totalPred.put(triple.predicate, totalPred.get(triple.predicate) + 1);
            if (!questionRelations.contains(triple) &&
                    !questionAnswerRelations.contains(triple)) {
                if (first) {
                    System.out.println(">>> Answer only");
                    first = false;
                }
                has = true;
                System.out.println(triple);
                ++answerOnly;
                answerOnlyPred.put(triple.predicate, answerOnlyPred.get(triple.predicate) + 1);
            }
        }
        first = true;
        for (KnowledgeBase.Triple triple : questionAnswerRelations) {
            if (!totalPred.containsKey(triple.predicate)) {
                totalPred.put(triple.predicate, 0);
                questionOnlyPred.put(triple.predicate, 0);
                answerOnlyPred.put(triple.predicate, 0);
                questionAnswerOnlyPred.put(triple.predicate, 0);
            }
            totalPred.put(triple.predicate, totalPred.get(triple.predicate) + 1);
            if (!questionRelations.contains(triple) &&
                    !answerRelations.contains(triple)) {
                if (first) {
                    System.out.println(">>> Question-Answer only");
                    first = false;
                }
                System.out.println(triple);
                has = true;
                ++questionAnswerOnly;
                questionAnswerOnlyPred.put(triple.predicate,
                        questionAnswerOnlyPred.get(triple.predicate) + 1);
            }
        }
        total += totalRelations.size();

        if (has) {
            System.out.println(document.getText());
            System.out.println("-----------------------------------------------------------------");
        }

        return document;
    }

    @Override
    public void finishProcessing() {
        for (Map.Entry<String, Integer> relCount : totalPred.entrySet()) {
            System.out.println(relCount.getKey() + "\t" +
                    questionOnlyPred.get(relCount.getKey()) + "\t" +
                    answerOnlyPred.get(relCount.getKey()) + "\t" +
                    questionAnswerOnlyPred.get(relCount.getKey()) + "\t" +
                    relCount.getValue());
        }
    }
}