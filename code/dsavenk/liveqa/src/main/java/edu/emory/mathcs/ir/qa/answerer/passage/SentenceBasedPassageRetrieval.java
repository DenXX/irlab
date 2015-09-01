package edu.emory.mathcs.ir.qa.answerer.passage;

import edu.emory.mathcs.ir.qa.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * Passage retrieval strategy that generates passages starting at the beginning
 * of a sentence and includes full sentences until the maximum length is
 * reached.
 */
public class SentenceBasedPassageRetrieval implements PassageRetrieval {
    @Override
    public Text[] getPassages(Text text, int maximumLength) {
        List<Text> passages = new ArrayList<>();

        for (int startSentence = 0; startSentence < text.getSentences().length;
             ++startSentence) {
            if (text.getSentences()[startSentence].tokens.length < 3) continue;

            final int startCharOffset =
                    text.getSentences()[startSentence].charBeginOffset;
            int endSentence = startSentence;

            // Move the end sentence index to the right while we still have
            // some characters left.
            while (endSentence < text.getSentences().length &&
                    text.getSentences()[endSentence].charEndOffset -
                            startCharOffset < maximumLength) {
                ++endSentence;
            }
            if (endSentence > startSentence) {
                passages.add(text.subtext(startSentence, endSentence - 1));
                startSentence = endSentence - 1;
            }
        }

        return passages.toArray(new Text[passages.size()]);
    }
}
