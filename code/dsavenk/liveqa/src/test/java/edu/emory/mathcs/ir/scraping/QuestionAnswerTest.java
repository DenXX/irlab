package edu.emory.mathcs.ir.scraping;

import junit.framework.TestCase;

import java.util.Optional;

/**
 * Created by dsavenk on 8/28/15.
 */
public class QuestionAnswerTest extends TestCase {

    public void testParseFromString() throws Exception {
        String qaStr = "id\tWhat is the title?\tWhat is the body\tThis is " +
                "the answer\tcategory1,category2,category3\t";
        YahooAnswersScraper.QuestionAnswer qa =
                YahooAnswersScraper.QuestionAnswer.parseFromString(qaStr);
        assertEquals("id", qa.qid);
        assertEquals("What is the title?", qa.title);
        assertEquals("What is the body", qa.body);
        assertEquals("This is the answer", qa.bestAnswer);
        assertEquals(3, qa.categories.length);
        assertEquals("category3", qa.categories[2]);
        assertEquals(0, qa.answers.length);
    }

    public void testToString() throws Exception {
        YahooAnswersScraper.QuestionAnswer qa =
                new YahooAnswersScraper.QuestionAnswer();
        qa.qid = "YA2011DHHPJPJ";
        qa.title = "I want\tto\nknow who is the president\n\n of the us?";
        qa.body = "";
        qa.bestAnswer = "\n\nI don't \n\n\treally know.";
        qa.categories = new String[]{"category", "subcategory"};
        qa.answers = new String[]{"I also don't know\n\r\t", "I\t don't care"};

        String qaStr = qa.toString();
        assertTrue(!qaStr.contains("\n"));
        assertEquals(6,
                org.apache.commons.lang.StringUtils.countMatches(qaStr, "\t"));
    }


    public void testToStringDownload() throws Exception {
        Optional<YahooAnswersScraper.QuestionAnswer> qa =
                YahooAnswersScraper.GetQuestionAnswerData(
                        "20130826043442AAaGniT");
        assertTrue(qa.isPresent());
        String qaStr = qa.get().toString();
        assertFalse(org.apache.commons.lang.StringUtils.contains(qaStr, "\n"));
    }
}