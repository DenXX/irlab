package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.Question;
import junit.framework.TestCase;

/**
 * Created by dsavenk on 8/24/15.
 */
public class TitleNoStopwordsQueryFormulatorTest extends TestCase {

    public void testGetQuery() throws Exception {
        Question q = new Question("",
                "What is the average salary of the software developer in GA?",
                "", "");
        TitleNoStopwordsQueryFormulator queryFormulator =
                new TitleNoStopwordsQueryFormulator();
        String query = queryFormulator.getQuery(q);
        assertTrue(query.contains("software"));
        assertTrue(query.contains("developer"));
        assertTrue(query.contains("salary"));
        assertTrue(query.contains(" ga"));
        assertTrue(!query.contains(" is "));
        assertTrue(!query.contains(" the "));
        assertTrue(!query.contains(" of "));
        assertTrue(!query.contains(" in "));
    }
}
