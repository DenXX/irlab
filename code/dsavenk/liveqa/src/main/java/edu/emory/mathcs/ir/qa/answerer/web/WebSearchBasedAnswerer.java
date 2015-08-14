package edu.emory.mathcs.ir.qa.answerer.web;

import edu.emory.mathcs.ir.qa.*;
import edu.emory.mathcs.ir.qa.answerer.AnswerFormatter;
import edu.emory.mathcs.ir.qa.answerer.AnswerRetrieval;
import edu.emory.mathcs.ir.qa.answerer.QuestionAnswering;
import edu.emory.mathcs.ir.qa.answerer.passage.PassageRetrieval;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerSelection;
import edu.emory.mathcs.ir.scraping.WebPageScraper;
import edu.emory.mathcs.ir.search.SearchResult;
import edu.emory.mathcs.ir.search.WebSearch;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

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
    private final PassageRetrieval passageRetrieval_;
    private final int maxLength;

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
                                  WebSearch search,
                                  PassageRetrieval passageRetrieval) {
        queryFormulator_ = queryFormulator;
        answerRanker_ = answerRanker;
        search_ = search;
        passageRetrieval_ = passageRetrieval;
        maxLength = Integer.parseInt(AppConfig.PROPERTIES.getProperty(
                AnswerFormatter.MAXIMUM_ANSWER_LENGTH));
    }

    @Override
    public Answer GetAnswer(Question question) {
        final Answer[] answerCandidates = retrieveAnswers(question);
        return answerRanker_.chooseBestAnswer(question, answerCandidates)
                .orElse(DEFAULT_ANSWER);
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

        // Generate passages from all retrieved documents.
        List<Answer> answers = new ArrayList<>();
        for (SearchResult result : results) {
            // Generate passages from the current documents.
            final Text[] currentPassages =
                    passageRetrieval_.getPassages(
                            new Text(result.content), maxLength);

            // Add passages to the list of answers.
            Arrays.stream(currentPassages)
                    .map(p -> new Answer(p, result.url))
                    .forEach(answers::add);
        }
        return answers.toArray(new Answer[answers.size()]);
    }
}
