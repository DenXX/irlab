package edu.emory.mathcs.ir.qa;

import junit.framework.TestCase;

import java.util.Arrays;

/**
 * Created by dsavenk on 8/13/15.
 */
public class TextTest extends TestCase {

    public void testGetSentences() throws Exception {
        Text text = new Text("Why am i not motivated to do anything? " +
                "Am i deppressed? \nI am 20 years old without a high school " +
                "diploma never had a girlfriend, have a shitty job and its " +
                "all because i have never been motived.");
        assertEquals(3, text.getSentences().length);
        assertEquals(39, text.getSentences()[1].charBeginOffset);
        assertEquals(4, text.getSentences()[1].tokens.length);
        assertEquals("i", text.getSentences()[1].tokens[1].lemma);
        assertEquals(42,
                text.getSentences()[1].tokens[1].charBeginOffset);
        assertEquals(43,
                text.getSentences()[1].tokens[1].charEndOffset);
        assertEquals("PRP", text.getSentences()[2].tokens[0].pos);
    }

    public void testGetEntities() throws Exception {
        if (AppConfig.PROPERTIES.getProperty(
                AppConfig.ANNOTATORS_PARAMETER).contains("ner")) {
            Text text = new Text("Freelance photographer Ben Adkison stayed " +
                    "up long into the night to catch this time-lapse " +
                    "video of the Perseid Meteor Shower. The video was shot " +
                    "at Brown Lake in western Montana's remote Blackfoot Valley.");
            assertTrue(Arrays.stream(text.getEntities())
                    .filter(e -> e.name.equals("Blackfoot Valley"))
                    .findFirst()
                    .isPresent());
            assertTrue(Arrays.stream(text.getEntities())
                    .filter(e -> e.name.equals("Montana"))
                    .findFirst()
                    .isPresent());
            assertTrue(Arrays.stream(text.getEntities())
                    .filter(e -> e.name.equals("Brown Lake"))
                    .findFirst()
                    .isPresent());
            assertTrue(Arrays.stream(text.getEntities())
                    .filter(e -> e.name.equals("Ben Adkison"))
                    .findFirst()
                    .isPresent());
            assertEquals(0, Arrays.stream(text.getEntities())
                    .filter(e -> e.name.equals("Ben Adkison"))
                    .findFirst()
                    .map(e -> e.mentions.get(0).sentenceIndex)
                    .get().intValue());
            assertEquals(1, Arrays.stream(text.getEntities())
                    .filter(e -> e.name.equals("Brown Lake"))
                    .findFirst()
                    .map(e -> e.mentions.get(0).sentenceIndex)
                    .get().intValue());
            assertEquals(5, Arrays.stream(text.getEntities())
                    .filter(e -> e.name.equals("Brown Lake"))
                    .findFirst()
                    .map(e -> e.mentions.get(0).beginToken)
                    .get().intValue());
            assertEquals(7, Arrays.stream(text.getEntities())
                    .filter(e -> e.name.equals("Brown Lake"))
                    .findFirst()
                    .map(e -> e.mentions.get(0).endToken)
                    .get().intValue());
        }
    }

    public void testSubtext() throws Exception {
        Text text = new Text("Freelance photographer Ben Adkison stayed " +
                "up long into the night to catch this time-lapse " +
                "video of the Perseid Meteor Shower. The video was shot " +
                "at Brown Lake in western Montana's remote Blackfoot Valley.");
        Text subtext = text.subtext(1, 1);
        assertEquals(1, subtext.getSentences().length);
        assertTrue(subtext.getSentences()[0].text.startsWith("The video"));
        assertEquals(subtext.text, subtext.getSentences()[0].text);
        if (AppConfig.PROPERTIES.getProperty(
                AppConfig.ANNOTATORS_PARAMETER).contains("ner")) {
            assertEquals(3, subtext.getEntities().length);
            assertTrue(!Arrays.stream(subtext.getEntities())
                    .filter(e -> e.name.equals("Ben Adkison"))
                    .findFirst()
                    .isPresent());
            assertTrue(Arrays.stream(subtext.getEntities())
                    .filter(e -> e.name.equals("Brown Lake"))
                    .findFirst()
                    .isPresent());
            assertTrue(Arrays.stream(subtext.getEntities())
                    .filter(e -> e.name.equals("Montana"))
                    .findFirst()
                    .isPresent());
            assertEquals(0, Arrays.stream(subtext.getEntities())
                    .filter(e -> e.name.equals("Brown Lake"))
                    .findFirst()
                    .map(e -> e.mentions.get(0).sentenceIndex)
                    .get().intValue());
            assertEquals(5, Arrays.stream(subtext.getEntities())
                    .filter(e -> e.name.equals("Brown Lake"))
                    .findFirst()
                    .map(e -> e.mentions.get(0).beginToken)
                    .get().intValue());
            assertEquals(7, Arrays.stream(subtext.getEntities())
                    .filter(e -> e.name.equals("Brown Lake"))
                    .findFirst()
                    .map(e -> e.mentions.get(0).endToken)
                    .get().intValue());
        }
    }

    public void testConcat() throws Exception {
        Text text1 = new Text("This is first text");
        Text text2 = new Text("This is second text");
        Text concat = text1.concat(text2);
        assertEquals(2, concat.getSentences().length);
        assertTrue(concat.text.contains("first"));
        assertTrue(concat.text.contains("second"));
    }

    public void testGetTokens() throws Exception {
        Text text = new Text("This is the first sentence. " +
                "This is the second sentence.");
        assertEquals(12, text.getTokens().length);
        assertTrue(Arrays.stream(text.getTokens()).anyMatch(
                token -> token.text.contains("first")));
        assertTrue(Arrays.stream(text.getTokens()).anyMatch(
                token -> token.text.contains("second")));
    }
}