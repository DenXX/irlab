package edu.emory.mathcs.ir.utils;

/**
 * Created by dsavenk on 8/10/15.
 */
public class StringUtils {
    /**
     * Normalizes a string by converting all characters to lowercase, replacing
     * all repetitive whitespaces with a single whitespaces and all tabs and
     * line breaks with spaces.
     * @param str A string to normalize.
     * @return The normalized string.
     */
    public static String normalizeString(String str) {
        return normalizeWhitespaces(str.toLowerCase());
    }

    /**
     * Removes all newline and tab characters and replaces repetitive
     * whitespaces with a single whitespace.
     * @param str A string to normalize.
     * @return String with whitespaces normalized.
     */
    public static String normalizeWhitespaces(String str) {
        return str.replace("\n", " ").replace("\t", " ")
                .replaceAll("\\s+", " ");
    }
}
