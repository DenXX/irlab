package edu.emory.mathcs.clir.relextract.utils;

import java.text.Normalizer;

/**
 * Created by dsavenk on 9/11/14.
 */
public final class NlpUtils {

    /**
     * Normalizes a string to be used for matches, more specifically the string
     * is lowercased, extra spaces are removed and non-ascii characters are
     * removed.
     * @param str A string to normalize.
     * @return Normalized string.
     */
    public static String normalizeStringForMatch(String str) {
        return Normalizer.normalize(str, Normalizer.Form.NFC).toLowerCase()
                .replaceAll("[^\\x20-\\x7E]+", " ").replaceAll("\\s+", " ");
    }
}
