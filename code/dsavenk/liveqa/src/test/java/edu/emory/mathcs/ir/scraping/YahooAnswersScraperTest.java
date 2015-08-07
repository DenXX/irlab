package edu.emory.mathcs.ir.scraping;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.InputStream;

/**
 * Created by dsavenk on 8/6/15.
 */
public class YahooAnswersScraperTest extends TestCase {

    public void testGetQuestionAnswerData() throws Exception {
        InputStream resourceStream =
                this.getClass().getResourceAsStream("/yahoo_answers_qna.html");
        YahooAnswersScraper.QuestionAnswerExtractor extractor =
                new YahooAnswersScraper.QuestionAnswerExtractor();
        extractor.Init();
        Document document = Jsoup.parse(resourceStream, "UTF-8", "test");
        WebPageScraper.processDocument(document,
                new WebPageScraper.ProcessElementCallback[]{extractor});
        YahooAnswersScraper.QuestionAnswer qna = extractor.getQuestionAnswer();
        Assert.assertEquals("20130830184555AA3Z07I", qna.qid);
        Assert.assertEquals("Will a rainbow shark and pleco be good in " +
                "a 30-38 gallon?", qna.title);
        Assert.assertTrue(qna.body.contains("Brown Dot Peckoltia"));
        Assert.assertEquals(2, qna.categories.length);
        Assert.assertEquals("Pets", qna.categories[0]);
        Assert.assertEquals("Fish", qna.categories[1]);
        Assert.assertEquals("", qna.bestAnswer);
        Assert.assertEquals(3, qna.answers.length);
        Assert.assertTrue(qna.answers[0].contains("needs 55 gallons"));
        Assert.assertTrue(qna.answers[1].contains("But once the shark and " +
                "Pleco get bigger"));
        Assert.assertTrue(qna.answers[2].contains(
                "http://www.aquariumforum.com/f4/"));
    }

    public void testGetRelatedQuestions() throws Exception {
        InputStream resourceStream =
                this.getClass().getResourceAsStream(
                        "/yahoo_answers_search.html");
        YahooAnswersScraper.SearchResultsExtractor extractor =
                new YahooAnswersScraper.SearchResultsExtractor();
        extractor.Init();
        Document document = Jsoup.parse(resourceStream, "UTF-8", "test");
        WebPageScraper.processDocument(document,
                new WebPageScraper.ProcessElementCallback[]{extractor});

        String[] qids = extractor.GetQids();
        Assert.assertEquals(10, qids.length);
        Assert.assertEquals("20090301162622AAmYBdX", qids[0]);
        Assert.assertEquals("20090516135743AAQrVRt", qids[7]);
    }
}