package edu.emory.mathcs.ir.search;

import org.carrot2.core.Document;

/**
 * Represents a web search result.
 */
public class SearchResult {
    public final int rank;
    public final String title;
    public final String url;
    public final String snippet;
    public String content = "";

    public SearchResult(int rank, String url, String title, String snippet) {
        this.rank = rank;
        this.url = url;
        this.title = title;
        this.snippet = snippet;
    }

    /**
     * Creates a search result given its rank and carrot2 Document object.
     * @param rank The rank of the search result.
     * @param document The Carrot2 document to convert to SearchResult.
     * @return An instance of SearchResult class with data fields copied from
     * the fields of the Carrot2 object.
     */
    public static SearchResult create(int rank, Document document) {
        return new SearchResult(rank,
                document.getField(Document.CONTENT_URL),
                document.getField(Document.TITLE),
                document.getField(Document.SUMMARY));
    }

    @Override
    public String toString() {
        return url;
    }
}
