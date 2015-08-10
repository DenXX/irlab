package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.question.Question;

/**
 * Generates queries for question by simply using their text.
 */
public class SimpleQueryFormulator implements QueryFormulation {
    @Override
    public String getQuery(Question question) {
        return question.getTitle().text.concat(question.getBody().text);
    }
}
