package edu.emory.mathcs.ir.qa.answerer.web;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.AnswerRetrieval;
import edu.emory.mathcs.ir.qa.answerer.QuestionAnswering;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerSelection;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.scraping.WebPageScraper;
import edu.emory.mathcs.ir.search.SearchResult;
import edu.emory.mathcs.ir.search.WebSearch;

import java.net.URL;
import java.util.Arrays;

/**
 * Uses web search for question answering by retrieving passages from the
 * web documents.
 */
public class WebSearchBasedAnswerer
        implements QuestionAnswering, AnswerRetrieval {
    private static final Answer DEFAULT_ANSWER =
            new Answer(new Text("I don't know"), "");
    public static final int WEB_SEARCH_RESULTS_TOP = 10;

    private final AnswerSelection answerRanker_;
    private final QueryFormulation queryFormulator_;
    private final WebSearch search_;

    /**
     * Creates an instance of the web search-based question answering system.
     * @param queryFormulator Object implementing QueryFormulation interface
     *                        to construct search queries for verbose questions.
     * @param answerRanker Candidate answers ranking strategy.
     * @param search Object implementing the WebSearch interface which is used
     *               to retrieve documents from which candidate answer passages
     *               are build.
     */
    public WebSearchBasedAnswerer(QueryFormulation queryFormulator,
                                  AnswerSelection answerRanker,
                                  WebSearch search) {
        queryFormulator_ = queryFormulator;
        answerRanker_ = answerRanker;
        search_ = search;
    }

    @Override
    public Answer GetAnswer(Question question) {
        return answerRanker_.chooseBestAnswer(
                question, retrieveAnswers(question)).orElse(DEFAULT_ANSWER);
    }

    @Override
    public Answer[] retrieveAnswers(Question question) {
        final String query = queryFormulator_.getQuery(question);

        // Get search results for the query.
        final SearchResult[] results  = search_.search(
                query, WEB_SEARCH_RESULTS_TOP);

        // Process results and add content to each document.
        Arrays.stream(results).forEach(res -> {
            try {
                res.content = WebPageScraper.getDocumentContent(
                        new URL(res.url));
            } catch (Exception e) {
                LiveQaLogger.LOGGER.warning(e.getMessage());
            }
        });

        return new Answer[0];
    }
}
