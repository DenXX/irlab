package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;

import java.util.Properties;

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
        int questionSentences = new DocumentWrapper(document).getQuestionSentenceCount();
        boolean hasNerInQuestions = false;
        for (Document.Span span : document.getSpanList()) {
            if (span.getType().equals("ENTITY")) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < questionSentences) {
                        if (document.getToken(document.getSentence(mention.getSentenceIndex()).getFirstToken()).getPos().startsWith("W"))
                            hasNerInQuestions = true;
                    }
                }
            }
        }
        if (!hasNerInQuestions) return null;

        return document;
    }
}
