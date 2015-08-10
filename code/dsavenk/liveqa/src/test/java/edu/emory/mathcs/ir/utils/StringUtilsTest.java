package edu.emory.mathcs.ir.utils;

import junit.framework.Assert;
import junit.framework.TestCase;

/**
 * Created by dsavenk on 8/10/15.
 */
public class StringUtilsTest extends TestCase {

    public void testNormalizeString() throws Exception {
        String str = "This Is a SAMple     strinG.";
        Assert.assertEquals("this is a sample string.",
                StringUtils.normalizeString(str));
    }

    public void testNormalizeMultilineString() throws Exception {
        String str = "This is".concat("\n").concat("a sample STRING.");
        Assert.assertEquals("this is a sample string.",
                StringUtils.normalizeString(str));
    }
}