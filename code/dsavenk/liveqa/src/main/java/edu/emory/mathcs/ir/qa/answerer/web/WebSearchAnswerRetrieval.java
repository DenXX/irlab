package edu.emory.mathcs.ir.qa.answerer.web;

import edu.emory.mathcs.ir.qa.*;
import edu.emory.mathcs.ir.qa.answerer.AnswerFormatter;
import edu.emory.mathcs.ir.qa.answerer.AnswerRetrieval;
import edu.emory.mathcs.ir.qa.answerer.passage.PassageRetrieval;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.scraping.WebPageScraper;
import edu.emory.mathcs.ir.search.SearchResult;
import edu.emory.mathcs.ir.search.WebSearch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Uses web search for question answering by retrieving passages from the
 * web documents.
 */
public class WebSearchAnswerRetrieval implements AnswerRetrieval {
    /**
     * The title of the webpage, where the answer was generated from.
     */
    public static final String PAGE_TITLE_ATTRIBUTE = "page_title";
    private final QueryFormulation[] queryFormulators_;
    private final WebSearch search_;
    private final PassageRetrieval passageRetrieval_;
    private final int maxLength_;
    private final int topN_;

    /**
     * Creates an instance of the web search-based question answering system.
     * @param queryFormulator Array of QueryFormulation strategies
     *                        to construct search queries for verbose questions.
     * @param search Object implementing the WebSearch interface which is used
     *               to retrieve documents from which candidate answer passages
     *               are build.
     */
    public WebSearchAnswerRetrieval(QueryFormulation[] queryFormulator,
                                    WebSearch search,
                                    PassageRetrieval passageRetrieval) {
        queryFormulators_ = queryFormulator;
        search_ = search;
        passageRetrieval_ = passageRetrieval;
        maxLength_ = Integer.parseInt(AppConfig.PROPERTIES.getProperty(
                AnswerFormatter.MAXIMUM_ANSWER_LENGTH));
        topN_ = Integer.parseInt(
                AppConfig.PROPERTIES.getProperty(
                        AppConfig.WEB_SEARCH_TOPN_PARAMETER));
    }

    @Override
    public Answer[] retrieveAnswers(Question question) {
        List<Answer> answers = new ArrayList<>();
        for (final QueryFormulation queryFormulator : queryFormulators_) {
            // Get search results for the query.
            final String query = queryFormulator.getQuery(question);
            final SearchResult[] results = search_.search(query, topN_);

            // Process results and add content to each document.
            Arrays.stream(results)
                    .parallel()
                    .forEach(res -> {
                        try {
                            res.content = WebPageScraper.getDocumentContent(
                                    new URL(res.url));
                        } catch (Exception e) {
                            LiveQaLogger.LOGGER.warning(e.getMessage());
                        }
                    });

            // Generate passages from all retrieved documents.
            for (SearchResult result : results) {
                // Generate passages from the current documents.
                final Text[] currentPassages =
                        passageRetrieval_.getPassages(
                                new Text(AnswerFormatter.formatAnswer(
                                        result.content)), maxLength_);

                // Add passages to the list of answers.
                Arrays.stream(currentPassages)
                        .map(p -> new Answer(p, result.url))
                        .map(a -> {
                            a.setAttribute(PAGE_TITLE_ATTRIBUTE, result.title);
                            return a;
                        })
                        .forEach(answers::add);
                Answer snippetAnswer = new Answer(
                        AnswerFormatter.formatAnswer(result.snippet),
                        result.url);
                snippetAnswer.setAttribute(PAGE_TITLE_ATTRIBUTE, result.title);
                answers.add(snippetAnswer);
            }
        }
        return answers.toArray(new Answer[answers.size()]);
    }
}
