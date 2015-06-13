package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;

import java.util.*;

/**
 * Created by denxx on 6/12/15.
 */
public class RecommendationExtractionProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public RecommendationExtractionProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        DocumentWrapper documentWrapper = new DocumentWrapper(document);
        boolean qaPrinted = false;

        List<String> answers = new ArrayList<>();
        for (Document.Span span : document.getSpanList()) {
            if (span.getType().equals("ENTITY")) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() >= documentWrapper.getQuestionSentenceCount()) {
                        answers.add(span.getText().replace("\t", " ").replace("\n", " "));
                    }
                }
            }

        }

        for (int tokenIndex = 0; tokenIndex < document.getTokenCount() &&
                document.getToken(tokenIndex).getBeginCharOffset() < document.getQuestionLength(); ++tokenIndex) {
            if (document.getToken(tokenIndex).getPos().equals("JJS") || document.getToken(tokenIndex).getLemma().equals("recommend")) {
                if (document.getToken(tokenIndex).getDependencyGovernor() > 0) {
                    if (!qaPrinted) {
                        StringBuilder question = new StringBuilder();
                        StringBuilder answer = new StringBuilder();
                        StringBuilder curBuilder = question;
                        for (int sentence = 0; sentence < document.getSentenceCount(); ++sentence) {
                            if (sentence == documentWrapper.getQuestionSentenceCount()) {
                                curBuilder = answer;
                            }
                            curBuilder.append(document.getSentence(sentence).getText().replace("\n", " ")).append(" ");
                        }
                        System.out.println(question.toString());
                        System.out.println(answer.toString());
                        qaPrinted = true;
                    }

                    int dependencyHead = document.getToken(tokenIndex).getDependencyGovernor() + document.getSentence(document.getToken(tokenIndex).getSentenceIndex()).getFirstToken() - 1;
                    String type = document.getToken(dependencyHead).getText();
                    String attributes = String.join(",", documentWrapper.getModifierPhrases(dependencyHead, new HashSet<>(Arrays.asList(tokenIndex))));
                    System.out.println(type + "\t" + attributes + "\t" + String.join(", ", answers));
                }
            }
            if (document.getToken(tokenIndex).getLemma().equals("recommend")) {
                int head = documentWrapper.getDependencyChild(tokenIndex, "dobj");
                if (head >= 0) {
                    if (!qaPrinted) {
                        StringBuilder question = new StringBuilder();
                        StringBuilder answer = new StringBuilder();
                        StringBuilder curBuilder = question;
                        for (int sentence = 0; sentence < document.getSentenceCount(); ++sentence) {
                            if (sentence == documentWrapper.getQuestionSentenceCount()) {
                                curBuilder = answer;
                            }
                            curBuilder.append(document.getSentence(sentence).getText().replace("\n", " ")).append(" ");
                        }
                        System.out.println(question.toString());
                        System.out.println(answer.toString());
                        qaPrinted = true;
                    }

                    String type = document.getToken(head).getText();
                    String attributes = String.join(",", documentWrapper.getModifierPhrases(head, new HashSet<>(Arrays.asList(tokenIndex))));
                    System.out.println(type + "\t" + attributes + "\t" + String.join(", ", answers));
                }
            }
        }
        if (qaPrinted) {
            System.out.println();
            return document;
        }
        return null;
    }
}
