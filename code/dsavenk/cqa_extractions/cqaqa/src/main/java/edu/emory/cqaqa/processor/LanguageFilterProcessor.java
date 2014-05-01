package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.CqaPost;
import edu.emory.cqaqa.types.QuestionAnswerPair;

/**
 * Filters posts of languages other than the provided one.
 */
public class LanguageFilterProcessor implements QuestionAnswerPairProcessor {
    private String languageToKeep;

    /**
     * Creates a processor and passes language of posts to keep.
     * @param languageToKeep Language of posts to keep.
     */
    public LanguageFilterProcessor(String languageToKeep) {
        this.languageToKeep = languageToKeep;
    }

    /**
     * Returns null if qa's language doesn't match the given language and the qa itself otherwise.
     * @param qa A qa to process.
     * @return Returns null if qa's language doesn't match the given language and the qa itself otherwise.
     */
    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair qa) {
        if (qa.getAttribute("language").equalsIgnoreCase(languageToKeep))
            return qa;
        return null;
    }
}
