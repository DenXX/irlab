package edu.emory.mathcs.ir.qa.answerer.passage;

import edu.emory.mathcs.ir.qa.Text;
import junit.framework.TestCase;

/**
 * Created by dsavenk on 8/13/15.
 */
public class SentenceBasedPassageRetrievalTest extends TestCase {

    public void testGetPassages() throws Exception {
        Text text = new Text("This is a sentence. And this is a sentence. " +
                "This is a long sentence which will be too long so we " +
                "should skip it. This sentence is just ok.");
        SentenceBasedPassageRetrieval passageRetrieval =
                new SentenceBasedPassageRetrieval();
        Text[] passages = passageRetrieval.getPassages(text, 50);
        assertEquals(2, passages.length);
        assertEquals(2, passages[0].getSentences().length);
    }
}