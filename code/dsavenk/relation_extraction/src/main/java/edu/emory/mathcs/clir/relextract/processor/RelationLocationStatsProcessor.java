package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by dsavenk on 10/15/14.
 */
public class RelationLocationStatsProcessor extends Processor {
    private int questionOnly = 0;
    private int answerOnly = 0;
    private int questionAnswerOnly = 0;
    private int total = 0;

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
//                if (!rel.getRelation().equals("location.location.contains")) continue;
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

            System.out.println("-----------------------------------------------------------------");
            total += totalRelations.size();
            System.out.println(document.getText());
            boolean first = true;
            for (String relation : questionRelations) {
                if (!answerRelations.contains(relation) &&
                        !questionAnswerRelations.contains(relation)) {
                    if (first) {
                        System.out.println(">>> Question only");
                        first = false;
                    }
                    System.out.println(relation);
                    ++questionOnly;
                }
            }
            first = true;
            for (String relation : answerRelations) {
                if (!questionRelations.contains(relation) &&
                        !questionAnswerRelations.contains(relation)) {
                    if (first) {
                        System.out.println(">>> Answer only");
                        first = false;
                    }
                    System.out.println(relation);
                    ++answerOnly;
                }
            }
            first = true;
            for (String relation : questionAnswerRelations) {
                if (!questionRelations.contains(relation) &&
                        !answerRelations.contains(relation)) {
                    if (first) {
                        System.out.println(">>> Question-Answer only");
                        first = false;
                    }
                    System.out.println(relation);
                    ++questionAnswerOnly;
                }
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
    }

    private String relationToString(Document.NlpDocument document,
                                    Document.Relation relation) {
        StringBuilder res = new StringBuilder();
        res.append(document.getSpan(relation.getSubjectSpan()).getText());
        res.append(" [");
        res.append(document.getSpan(relation.getSubjectSpan()).getEntityId());
        res.append(" ]");
        res.append(" - ");
        res.append(relation.getRelation());
        res.append(" - ");
        res.append(document.getSpan(relation.getObjectSpan()).getValue());
        if (document.getSpan(relation.getObjectSpan()).hasEntityId()) {
            res.append("[ ");
            res.append(document.getSpan(relation.getObjectSpan()).getEntityId());
            res.append(" ]");
        }
        return res.toString();
    }
}