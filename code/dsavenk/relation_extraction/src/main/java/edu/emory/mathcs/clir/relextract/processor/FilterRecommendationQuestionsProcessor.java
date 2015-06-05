package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;

import java.util.Properties;

/**
 * Created by denxx on 6/4/15.
 */
public class FilterRecommendationQuestionsProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public FilterRecommendationQuestionsProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        for (int tokenIndex = 0; tokenIndex < document.getTokenCount(); ++tokenIndex) {
            if (document.getToken(tokenIndex).getBeginCharOffset() >= document.getQuestionLength()) break;
            if (document.getToken(tokenIndex).getPos().equals("JJS")) {
                return document;
            }
        }
        return null;
    }
}
