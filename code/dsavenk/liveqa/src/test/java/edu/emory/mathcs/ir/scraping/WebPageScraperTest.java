package edu.emory.mathcs.ir.scraping;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;

/**
 * Tests web scraper functionality.
 */
public class WebPageScraperTest extends TestCase {

    public void testProcessDocument() throws Exception {
        InputStream resourceStream =
                this.getClass().getResourceAsStream("/yahoo_answers_qna.html");
        Document document = Jsoup.parse(resourceStream, "UTF-8", "test");
        final String[] qid = {""};
        final String[] title = {""};
        final String[] body = {""};
        final String[] category = {""};
        WebPageScraper.processDocument(document,
                new WebPageScraper.ProcessElementCallback[]{
                        e -> {
                            if (e.nodeName().equals("meta") &&
                                    e.attr("name").equals("title")) {
                                title[0] = e.attr("content");
                            } else if (e.nodeName().equals("meta") &&
                                    e.attr("name").equals("description")) {
                                body[0] = e.attr("content");
                            } else if (e.nodeName().equals("a") &&
                                    e.className().contains("Clr-b") &&
                                    e.parent().id().equals("brdCrb") &&
                                    e.previousElementSibling() == null) {
                                category[0] = e.text();
                            } else if (e.nodeName().equals("div") &&
                                    e.hasAttr("data-ya-question-id")) {
                                qid[0] = e.attr("data-ya-question-id");
                            }
                        }
                }
        );
        Assert.assertEquals("20130830184555AA3Z07I", qid[0]);
        Assert.assertEquals("Will a rainbow shark and pleco be good in " +
                "a 30-38 gallon?", title[0]);
        Assert.assertTrue(body[0].contains("Brown Dot Peckoltia"));
        Assert.assertEquals("Pets", category[0]);
    }

    public void testGetDocumentContent() throws Exception {
        InputStream resourceStream =
                this.getClass().getResourceAsStream("/test.html");
        String content = WebPageScraper.getDocumentContent(resourceStream);
        // Contains the title of the article.
        Assert.assertTrue(
                content.contains("9 Ways to Take Your Diet on Vacation"));
        // Contains subtitle of the article.
        Assert.assertTrue(
                content.contains("Food and travel don't have to add up to " +
                        "diet disaster."));
        // Contains the first sentence of the article.
        Assert.assertTrue(
                content.contains("When you take a trip, does your diet go on " +
                        "vacation, too?"));
        // Contains the last sentence of the article.
        Assert.assertTrue(
                content.contains("You can also find a local market and " +
                        "stock up on fresh fruit to have in your hotel room " +
                        "for breakfast and snacks."));
    }

    public void testGetDocumentContentAnotherPage() throws Exception {
        InputStream resourceStream =
                this.getClass().getResourceAsStream("/test2.html");
        String content = WebPageScraper.getDocumentContent(resourceStream);
        // Contains the title of the article.
        Assert.assertTrue(
                content.contains("Underwear (Base Layer): How to Choose"));
        Assert.assertTrue(
                content.contains("Want a comfort boost on your next " +
                        "outdoor adventure?"));
        Assert.assertTrue(
                content.contains("Admittedly, these nuances can be tough " +
                        "to detect in the field, and when conditions turn " +
                        "seriously cold, you will obviously need more than " +
                        "a lightweight wool tee to maintain a comfortable " +
                        "body temperature."));
    }
}