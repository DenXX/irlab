package edu.emory.mathcs.ir.search;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Created by dsavenk on 8/11/15.
 */
public class BingWebSearchTest extends TestCase {

    public void testSearch() throws Exception {
        BingWebSearch searcher = new BingWebSearch();
        SearchResult[] res = searcher.search("google", 1);
        Assert.assertEquals(1, res.length);
        Assert.assertEquals("http://www.google.com/", res[0].url);
    }
}