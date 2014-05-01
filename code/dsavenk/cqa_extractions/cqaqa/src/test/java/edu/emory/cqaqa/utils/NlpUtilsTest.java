package edu.emory.cqaqa.utils;

import edu.emory.cqaqa.utils.NlpUtils;
import edu.stanford.nlp.ling.CoreLabel;
import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.io.IOException;
import java.util.List;

public class NlpUtilsTest extends TestCase
{
    /**
     * Create the test case
     *
     * @param testName name of the test case
     */
    public NlpUtilsTest( String testName )
    {
        super( testName );
    }

    /**
     * @return the suite of tests being tested
     */
    public static Test suite()
    {
        return new TestSuite( NlpUtilsTest.class );
    }

    public void testNamedEntityDetection()
    {
        try {
            String question = "What is the capital city of Czech Republic? How old was James Bonhan when he died?";
            List<List<CoreLabel>> res = NlpUtils.detectEntities(question);
            assertEquals(2, res.size());
        } catch (Exception e) {
            fail();
        }
    }
}
