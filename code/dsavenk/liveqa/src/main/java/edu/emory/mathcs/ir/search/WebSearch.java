package edu.emory.mathcs.ir.search;

/**
 * Web search interface, which contains a method returning a set of search
 * results given a search query.
 */
public interface WebSearch {
    /**
     * Returns a list of up to top search results for the given query.
     * @param query Search query.
     * @param top Upper limit on the number of results to return.
     * @return An array of search results.
     */
    SearchResult[] search(String query, int top);
}
