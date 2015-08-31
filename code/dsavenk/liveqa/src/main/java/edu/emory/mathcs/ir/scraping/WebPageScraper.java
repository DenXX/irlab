package edu.emory.mathcs.ir.scraping;

import de.l3s.boilerpipe.document.TextDocument;
import edu.emory.mathcs.ir.qa.LiveQaLogger;
import org.apache.commons.httpclient.DefaultHttpMethodRetryHandler;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.httpclient.methods.GetMethod;
import org.apache.commons.httpclient.params.HttpMethodParams;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.html.BoilerpipeContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;

/**
 * Downloads and parses the content of a web page.
 */
public class WebPageScraper {

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
            LiveQaLogger.LOGGER.warning("Method failed: " + method.getStatusLine());
        }
        InputStream responseBody = method.getResponseBodyAsStream();
        return Jsoup.parse(responseBody, "UTF8", url.toExternalForm());
    }

    /**
     * Returns the main content of the web document given a URL.
     * @param url URL of web document to extract.
     * @return String representation of the web document main content.
     * @throws IOException
     * @throws TikaException
     */
    public static String getDocumentContent(URL url)
            throws IOException, TikaException, SAXException {
        return getDocumentContent(url.openStream());
    }

    /**
     * Returns the main content of the web document given a URL.
     * @param stream Input stream for the document content.
     * @return String representation of the main document content.
     * @throws IOException
     * @throws TikaException
     */
    public static String getDocumentContent(InputStream stream)
            throws IOException, TikaException, SAXException {
        BoilerpipeContentHandler handler = new BoilerpipeContentHandler(
                new BodyContentHandler());
        AutoDetectParser parser = new AutoDetectParser();
        parser.parse(stream, handler, new Metadata());
        final TextDocument doc = handler.toTextDocument();
        return doc.getContent();
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


    /**
     * Interface for a callback that determines which web page elements need to
     * be processed and extracts elements of interest.
     */
    public interface ProcessElementCallback {
        /**
         * Processes the given web page element if needed.
         *
         * @param e An element to process.
         */
        void processElement(Element e);
    }
}
