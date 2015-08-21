package edu.emory.mathcs.ir.utils;

import edu.stanford.nlp.pipeline.Annotation;
import junit.framework.TestCase;

import java.util.List;

/**
 * Created by dsavenk on 8/21/15.
 */
public class NlpUtilsTest extends TestCase {

    public void testGetAnnotations() throws Exception {
        final String question = "What's going on with my teeth? My mouth " +
                "hurts so friggin bad! I thought my wisdom teeth were done " +
                "growing in, but there's this tooth coming in behind " +
                "a wisdom tooth on the upper right-hand side and any time " +
                "I breath, talk, swallow or anything that puts " +
                "the slightest bit of pressure over there it hurts. " +
                "I've broken bones that weren't this awful and annoying. " +
                "What is up with this tooth though? Why does my jaw hurt " +
                "so much and what is it? I thought we only got one wisdom " +
                "on each side. Please, someone explain this.";
        Annotation text = NlpUtils.getAnnotations(question);
        List<String> chunks = NlpUtils.getChunks(text);
    }
}