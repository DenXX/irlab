package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.question.Question;

/**
 * Defines a method to formulate a query given a question.
 */
public interface QueryFormulation {
    /**
     * Generates a search query for the given question.
     * @param question The question used to generate the query.
     * @return A string query which can be used to query a search engine.
     */
    String getQuery(Question question);
}
