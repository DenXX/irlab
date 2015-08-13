package edu.emory.mathcs.ir.qa.answerer.passage;

import edu.emory.mathcs.ir.qa.Text;

/**
 * Passage retrieval strategy that generates passages starting at the beginning
 * of a sentence and includes full sentences until the maximum length is
 * reached.
 */
public class SentenceBasedPassageRetrieval implements PassageRetrieval {
    @Override
    public Text[] getPassages(Text text, int maximumLength) {

        return new Text[0];
    }
}
