package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.question.Question;
import edu.emory.mathcs.ir.utils.StringUtils;

/**
 * Creates a query for the given question by taking its title only.
 */
public class TitleOnlyQueryFormulator implements QueryFormulation {
    @Override
    public String getQuery(Question question) {
        return StringUtils.normalizeString(question.getTitle().text);
    }
}
