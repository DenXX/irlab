package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.*;

/**
 * Created by dsavenk on 10/15/14.
 */
public class RelationLocationStatsProcessor extends Processor {
    private int questionOnly = 0;
    private Map<String, Integer> questionOnlyCat = new HashMap<>();
    private Map<String, Integer> questionOnlyPred = new HashMap<>();
    private int answerOnly = 0;
    private Map<String, Integer> answerOnlyCat = new HashMap<>();
    private Map<String, Integer> answerOnlyPred = new HashMap<>();
    private int questionAnswerOnly = 0;
    private Map<String, Integer> questionAnswerOnlyCat = new HashMap<>();
    private Map<String, Integer> questionAnswerOnlyPred = new HashMap<>();
    private int total = 0;
    private Map<String, Integer> totalCat = new HashMap<>();
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
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        int questionLengthChars = document.getQuestionLength();
        String category = "";
        for (Document.Attribute attr : document.getAttributeList()) {
            if (attr.getKey().equals("maincat")) category = attr.getValue();
        }
        if (!questionOnlyCat.containsKey(category)) {
            questionOnlyCat.put(category, 0);
            questionAnswerOnlyCat.put(category, 0);
            answerOnlyCat.put(category, 0);
            totalCat.put(category, 0);
        }
        int questionSentencesCount = 0;
        for (Document.Sentence sent : document.getSentenceList()) {
            if (document.getToken(sent.getFirstToken()).getBeginCharOffset()
                    >= questionLengthChars) {
                break;
            }
            ++questionSentencesCount;
        }

        Set<String> questionRelations = new HashSet<>();
        Set<String> questionAnswerRelations = new HashSet<>();
        Set<String> answerRelations = new HashSet<>();
        Set<String> totalRelations = new HashSet<>();
        if (document.getRelationCount() > 0) {
            for (Document.Relation rel : document.getRelationList()) {
//                if (!rel.getRelation().equals("people.person.date_of_birth")) continue;
                boolean inQuestion = false;
                boolean inAnswer = false;
                boolean inQuestionAnswer = false;
                for (Document.Mention mention1 : document.getSpan(
                        rel.getSubjectSpan()).getMentionList()) {
                    boolean subjInQuestion = mention1.getSentenceIndex() <
                            questionSentencesCount;
                    for (Document.Mention mention2 : document.getSpan(
                            rel.getObjectSpan()).getMentionList()) {
                        boolean objInQuestion = mention2.getSentenceIndex() <
                                questionSentencesCount;
                        if (subjInQuestion) {
                            if (objInQuestion) {
                                inQuestion = true;
                            } else {
                                inQuestionAnswer = true;
                            }
                        } else if (objInQuestion) {
                            inQuestionAnswer = true;
                        } else {
                            inAnswer = true;
                        }
                    }
                }
                totalRelations.add(relationToString(document, rel));
                if (inQuestion && !inAnswer && !inQuestionAnswer) {
                    questionRelations.add(relationToString(document, rel));
                } else if (inAnswer && !inQuestion && !inQuestionAnswer) {
                    answerRelations.add(relationToString(document, rel));
                } else if (inQuestionAnswer && !inQuestion && !inAnswer) {
                    questionAnswerRelations.add(relationToString(document, rel));
                }
            }

            boolean has = false;
            boolean first = true;
            for (String relation : questionRelations) {
                if (!totalPred.containsKey(relation)) {
                    totalPred.put(relation, 0);
                    questionOnlyPred.put(relation, 0);
                    answerOnlyPred.put(relation, 0);
                    questionAnswerOnlyPred.put(relation, 0);
                }

                totalPred.put(relation, totalPred.get(relation) + 1);
                if (!answerRelations.contains(relation) &&
                        !questionAnswerRelations.contains(relation)) {
                    if (first) {
                        System.out.println(">>> Question only");
                        first = false;
                    }
                    has = true;
                    System.out.println(relation);
                    ++questionOnly;
                    questionOnlyCat.put(category, questionOnlyCat.get(category) + 1);
                    questionOnlyPred.put(relation, questionOnlyPred.get(relation) + 1);
                }
            }
            first = true;
            for (String relation : answerRelations) {
                if (!totalPred.containsKey(relation)) {
                    totalPred.put(relation, 0);
                    questionOnlyPred.put(relation, 0);
                    answerOnlyPred.put(relation, 0);
                    questionAnswerOnlyPred.put(relation, 0);
                }
                totalPred.put(relation, totalPred.get(relation) + 1);
                if (!questionRelations.contains(relation) &&
                        !questionAnswerRelations.contains(relation)) {
                    if (first) {
                        System.out.println(">>> Answer only");
                        first = false;
                    }
                    has = true;
                    System.out.println(relation);
                    ++answerOnly;
                    answerOnlyCat.put(category, answerOnlyCat.get(category) + 1);
                    answerOnlyPred.put(relation, answerOnlyPred.get(relation) + 1);
                }
            }
            first = true;
            for (String relation : questionAnswerRelations) {
                if (!totalPred.containsKey(relation)) {
                    totalPred.put(relation, 0);
                    questionOnlyPred.put(relation, 0);
                    answerOnlyPred.put(relation, 0);
                    questionAnswerOnlyPred.put(relation, 0);
                }
                totalPred.put(relation, totalPred.get(relation) + 1);
                if (!questionRelations.contains(relation) &&
                        !answerRelations.contains(relation)) {
                    if (first) {
                        System.out.println(">>> Question-Answer only");
                        first = false;
                    }
                    System.out.println(relation);
                    has = true;
                    ++questionAnswerOnly;
                    questionAnswerOnlyCat.put(category, questionAnswerOnlyCat.get(category) + 1);
                    questionAnswerOnlyPred.put(relation, questionAnswerOnlyPred.get(relation) + 1);
                }
            }
            total += totalRelations.size();
            totalCat.put(category, totalCat.get(category) + totalRelations.size());

            if (has) {
                System.out.println("-----------------------------------------------------------------");
                System.out.println(document.getText());
            }

            return document;
        }
        return null;
    }

    @Override
    public void finishProcessing() {
        System.out.println("\n\n\n\nQuestion only %: " + questionOnly + " (" + (1.0 * questionOnly / total) + " )");
        System.out.println("Answer only %: " + answerOnly + " (" + (1.0 * answerOnly / total) + " )");
        System.out.println("Question-Answer only %: " + questionAnswerOnly + " (" + (1.0 * questionAnswerOnly / total) + " )");

        for (Map.Entry<String, Integer> relCount : totalPred.entrySet()) {
            if (relCount.getValue() < 100) continue;
            System.out.println("\n\n!!!" + relCount.getKey() + "\n\nQuestion only %: " + questionOnlyPred.get(relCount.getKey()) + " (" + (1.0 * questionOnlyPred.get(relCount.getKey()) / relCount.getValue()) + " )");
            System.out.println("Answer only %: " + answerOnlyPred.get(relCount.getKey()) + " (" + (1.0 * answerOnlyPred.get(relCount.getKey()) / relCount.getValue()) + " )");
            System.out.println("Question-Answer only %: " + questionAnswerOnlyPred.get(relCount.getKey()) + " (" + (1.0 * questionAnswerOnlyPred.get(relCount.getKey()) / relCount.getValue()) + " )");
        }

//        for (Map.Entry<String, Integer> category : totalCat.entrySet()) {
//            if (category.getValue() < 100) continue;
//            System.out.println("\n\n!!!" + category + "\n\nQuestion only %: " + questionOnlyCat.get(category.getKey()) + " (" + (1.0 * questionOnlyCat.get(category.getKey()) / category.getValue()) + " )");
//            System.out.println("Answer only %: " + answerOnlyCat.get(category.getKey()) + " (" + (1.0 * answerOnlyCat.get(category.getKey()) / category.getValue()) + " )");
//            System.out.println("Question-Answer only %: " + questionAnswerOnlyCat.get(category.getKey()) + " (" + (1.0 * questionAnswerOnlyCat.get(category.getKey()) / category.getValue()) + " )");
//        }
    }

    private String relationToString(Document.NlpDocument document,
                                    Document.Relation relation) {
        StringBuilder res = new StringBuilder();
//        res.append(document.getSpan(relation.getSubjectSpan()).getText());
//        res.append(" [");
//        res.append(document.getSpan(relation.getSubjectSpan()).getEntityId());
//        res.append(" ]");
//        res.append(" - ");
        res.append(relation.getRelation());
//        res.append(" - ");
//        res.append(document.getSpan(relation.getObjectSpan()).getValue());
//        if (document.getSpan(relation.getObjectSpan()).hasEntityId()) {
//            res.append("[ ");
//            res.append(document.getSpan(relation.getObjectSpan()).getEntityId());
//            res.append(" ]");
//        }
        return res.toString();
    }
}