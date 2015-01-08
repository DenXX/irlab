package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;

import java.util.Properties;

/**
 * Created by dsavenk on 1/8/15.
 */
public class OutputQuestionTemplateProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public OutputQuestionTemplateProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        int questionSentencesCount = 0;
        while (questionSentencesCount < document.getSentenceCount()) {
            if (document.getToken(document.getSentence(questionSentencesCount).getFirstToken()).getBeginCharOffset() >= document.getQuestionLength()) {
                break;
            }
            ++questionSentencesCount;
        }
        if (questionSentencesCount == 1
                && document.getSentenceCount() == 2) {
            int inQuestionCount = 0;
            Document.Span questionEntitySpan = null;
            int questionEntityMentionIndex = 0;
            for (Document.Span span : document.getSpanList()) {
                if (span.getType().equals("OTHER")) continue;
                boolean inQuestion = false;
                int i = 0;
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() == 0) {
                        inQuestion = true;
                        questionEntityMentionIndex = i;
                        questionEntitySpan = span;
                    }
                    ++i;
                }
                if (inQuestion) ++inQuestionCount;
            }
            if (inQuestionCount == 1) {
                System.out.println(NlpUtils.getQuestionTemplate(document, 0, questionEntitySpan, questionEntityMentionIndex));
                return document;
            }
        }
        return null;
    }
}
