package edu.emory.mathcs.ir.qa.answerer.web;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.passage.SentenceBasedPassageRetrieval;
import edu.emory.mathcs.ir.qa.answerer.query.SimpleQueryFormulator;
import edu.emory.mathcs.ir.qa.answerer.ranking.QuestionTermsCountAnswerSelector;
import edu.emory.mathcs.ir.search.BingWebSearch;
import junit.framework.TestCase;

/**
 * Created by dsavenk on 8/14/15.
 */
public class WebSearchBasedAnswererTest extends TestCase {

    public void testGetAnswer() throws Exception {
        final WebSearchBasedAnswerer answerer =
                new WebSearchBasedAnswerer(
                        new SimpleQueryFormulator(false, true),
                        new QuestionTermsCountAnswerSelector(),
                        new BingWebSearch(),
                        new SentenceBasedPassageRetrieval());
        final Question q = new Question(
                "", "What is the capital of the US?", "", "General Knowledge");
        final Answer answer = answerer.GetAnswer(q);
        assertTrue(answer.getAnswer().text.contains("Washington"));
    }
}