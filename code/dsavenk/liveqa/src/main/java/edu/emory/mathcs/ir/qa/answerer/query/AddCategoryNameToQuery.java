package edu.emory.mathcs.ir.qa.answerer.query;


import edu.emory.mathcs.ir.qa.Question;

/**
 * Created by dsavenk on 8/31/15.
 */
public class AddCategoryNameToQuery implements QueryFormulation {
    private QueryFormulation queryFormulator_;

    public AddCategoryNameToQuery(QueryFormulation queryFormulator) {
        this.queryFormulator_ = queryFormulator;
    }

    @Override
    public String getQuery(Question question) {
        return queryFormulator_.getQuery(question) + " " +
                question.getCategory();
    }
}
