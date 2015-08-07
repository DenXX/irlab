package edu.emory.mathcs.ir.scraping;

import junit.framework.Assert;
import junit.framework.TestCase;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.InputStream;

/**
 * Created by dsavenk on 8/6/15.
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
}