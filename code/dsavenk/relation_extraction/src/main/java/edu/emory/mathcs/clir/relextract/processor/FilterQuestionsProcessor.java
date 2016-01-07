package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by dsavenk on 5/1/15.
 */
public class FilterQuestionsProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public FilterQuestionsProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        DocumentWrapper docWrapper = new DocumentWrapper(document);
        int questionSentences = docWrapper.getQuestionSentenceCount();
        if (questionSentences != 1) return null;
        if (!document.getToken(document.getSentence(0).getFirstToken()).getPos().startsWith("W") ||
                document.getToken(document.getSentence(0).getFirstToken()).getLemma().toLowerCase().equals("why")) return null;

        if (document.getToken(document.getSentence(0).getFirstToken()).getLemma().toLowerCase().equals("how")) {
            if (document.getToken(document.getSentence(0).getFirstToken() + 1).getPos().equals("TO") ||
                        document.getToken(document.getSentence(0).getFirstToken() + 1).getPos().startsWith("V") ||
                        document.getToken(document.getSentence(0).getFirstToken() + 1).getPos().startsWith("MD")) {
                return null;
            }
        }

        for (int token = document.getSentence(0).getFirstToken(); token < document.getSentence(0).getLastToken(); ++token) {
            if (shouldSkipByLemma(document.getToken(token).getLemma().toLowerCase())) {
                return null;
            }

            if (document.getToken(token).getPos().equals("JJR") ||
                    document.getToken(token).getPos().equals("JJS")) {
                return null;
            }
        }


        Set<String> questionEntityMids = new HashSet<>();
        Set<String> answerEntityMids = new HashSet<>();
        for (Document.Span span : document.getSpanList()) {
            if (span.getType().equals("ENTITY") && span.hasEntityId()) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < questionSentences) {
                        questionEntityMids.add(span.getEntityId());
                    } else {
                        answerEntityMids.add(span.getEntityId());
                    }
                }
            }
        }
        answerEntityMids.removeAll(questionEntityMids);
        if (questionEntityMids.isEmpty() || answerEntityMids.isEmpty()) return null;

        return document;
    }

    private static Set<String> stopLemmas = new HashSet<>();

    static {
        stopLemmas.add("you");
        stopLemmas.add("i");
        stopLemmas.add("we");
        stopLemmas.add("good");
        stopLemmas.add("will");
        stopLemmas.add("'ll");
    }

    private boolean shouldSkipByLemma(String lemma) {
        return stopLemmas.contains(lemma);
    }
}
