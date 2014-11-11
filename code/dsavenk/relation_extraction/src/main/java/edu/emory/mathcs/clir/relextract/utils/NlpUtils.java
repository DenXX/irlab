package edu.emory.mathcs.clir.relextract.utils;

import java.text.Normalizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by dsavenk on 9/11/14.
 */
public final class NlpUtils {

    public static String unquoteName(String name) {
        Matcher m = Pattern.compile("\\$([0-9A-Fa-f]{4})").matcher(name);
        while (m.find()) {
            name = name.replace(m.group(), Character.toString(
                    (char) Integer.parseInt(m.group(1), 16)));
        }
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
    }

    /**
     * Normalizes a string to be used for matches, more specifically the string
     * is lowercased, extra spaces are removed and non-ascii characters are
     * removed.
     *
     * @param str A string to normalize.
     * @return Normalized string.
     */
    public static String normalizeStringForMatch(String str) {
        return NlpUtils.unquoteName(str).replaceAll("[^A-Za-z0-9]", "")
                .toLowerCase();
    }
}
