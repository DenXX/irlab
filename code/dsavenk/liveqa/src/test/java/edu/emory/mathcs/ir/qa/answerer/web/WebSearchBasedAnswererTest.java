package edu.emory.mathcs.ir.qa.answerer.web;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.GenericQuestionAnswerer;
import edu.emory.mathcs.ir.qa.answerer.passage.SentenceBasedPassageRetrieval;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.answerer.query.SimpleQueryFormulator;
import edu.emory.mathcs.ir.qa.answerer.ranking.QuestionTermsCountAnswerSelector;
import edu.emory.mathcs.ir.search.BingWebSearch;
import junit.framework.TestCase;

/**
 * Created by dsavenk on 8/14/15.
 */
public class WebSearchBasedAnswererTest extends TestCase {

    public void testGetAnswer() throws Exception {
        final WebSearchAnswerRetrieval answerRetrieval =
                new WebSearchAnswerRetrieval(
                        new QueryFormulation[]{
                                new SimpleQueryFormulator(false, true)
                        },
                        new BingWebSearch(),
                        new SentenceBasedPassageRetrieval());
        final GenericQuestionAnswerer answerer = new GenericQuestionAnswerer(
                answerRetrieval, new QuestionTermsCountAnswerSelector());
        final Question q = new Question(
                "", "What is the capital of the US?", "", "General Knowledge");
        final Answer answer = answerer.GetAnswer(q);
        //assertTrue(answer.getAnswer().text.contains("Washington"));
    }
}
