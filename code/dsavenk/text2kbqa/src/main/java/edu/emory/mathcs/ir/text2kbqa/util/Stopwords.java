package edu.emory.mathcs.ir.text2kbqa.util;

import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by dsavenk on 12/17/15.
 */
public class Stopwords {
    private static Set<String> stopwords = new HashSet<>();

    static {
        Properties props = new Properties();
        try {
            props.load(Stopwords.class.getClassLoader().getResourceAsStream(
                    "config.properties"));
            for (String word : props.getProperty("stopwords").split(",")) {
                stopwords.add(word);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean isStopword(String word) {
        return stopwords.contains(word);
    }
}
