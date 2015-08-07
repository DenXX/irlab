package edu.emory.mathcs.ir.scraping;

import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Downloads and parses the content of a web page.
 */
public class WebPageScraper {

    /**
     * Interface for a callback that determines which web page elements need to
     * be processed and extracts elements of interest.
     */
    public interface ProcessElementCallback {
        /**
         * Processes the given web page element if needed.
         * @param e An element to process.
         */
        void processElement(Element e);
    }

    /**
     * Downloads the web document for the given url. The document is
     * parsed and callbacks are used on each DOM tree element to extract
     * elements of interest.
     * @param url URL of a web page to download and extract content from.
     * @param callbacks The list of callbacks.
     * @throws IOException
     */
    public static void scrape(URL url, ProcessElementCallback[] callbacks)
            throws IOException {
        if (callbacks == null || callbacks.length == 0) {
            throw new IllegalArgumentException(
                    "The list of callbacks cannot be empty");
        }
        Document doc = getDocument(url);
        processDocument(doc, callbacks);
    }

    /**
     * Processes parsed document using an array of callbacks that are called for
     * each DOM tree node to extract elements of interest.
     * @param document Parsed document.
     * @param callbacks The list of callbacks.
     * @throws IOException
     */
    public static void processDocument(Document document,
                              ProcessElementCallback[] callbacks)
            throws IOException {
        if (callbacks == null || callbacks.length == 0) {
            throw new IllegalArgumentException(
                    "The list of callbacks cannot be empty");
        }
        processElementHierarchy(document.head(), callbacks);
        processElementHierarchy(document.body(), callbacks);
    }

    /**
     * Downloads the document for the given URL, parses it and returns the DOM
     * Document.
     * @param url URL of a web page to scrape.
     * @return DOM document of the parsed web page.
     */
    public static Document getDocument(URL url) throws IOException {
        HttpClient client = new HttpClient();
        GetMethod method = new GetMethod(url.toString());
        method.getParams().setParameter(HttpMethodParams.RETRY_HANDLER,
                new DefaultHttpMethodRetryHandler(3, false));

        int statusCode = client.executeMethod(method);
        if (statusCode != HttpStatus.SC_OK) {
            logger.warning("Method failed: " + method.getStatusLine());
        }
        InputStream responseBody = method.getResponseBodyAsStream();
        return Jsoup.parse(responseBody, "UTF8", url.toExternalForm());
    }

    /**
     * Process the hierarchy of web page elements starting from the given top
     * element.
     * @param topElement The top element of the element tree.
     * @param callbacks A list of callbacks that will be test and extract
     *                  elements.
     */
    private static void processElementHierarchy(
            Element topElement,  ProcessElementCallback[] callbacks) {
        // Check the current element against callbacks.
        Arrays.stream(callbacks).forEach(
                callback -> callback.processElement(topElement));

        // Process children of the current node.
        for (Element childElement : topElement.children()) {
            processElementHierarchy(childElement, callbacks);
        }
    }

    private static Logger logger =
            Logger.getLogger(WebPageScraper.class.getName());
}
