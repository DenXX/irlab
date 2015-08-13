package edu.emory.mathcs.ir.qa.answerer.passage;

import edu.emory.mathcs.ir.qa.Text;

/**
 * Interface for building passages of the fixed maximum length from a long text.
 */
public interface PassageRetrieval {
    /**
     * Retrives a set of passages of the fixed maximum length from the given
     * text.
     * @param text Text to extract passages from.
     * @param maximumLength Maximum length of the passages.
     * @return A set of passages.
     */
    Text[] getPassages(Text text, int maximumLength);
}
