package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Document;
import org.apache.lucene.analysis.core.StopAnalyzer;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
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

    public static StringBuilder getQuestionTemplate(Document.NlpDocument document,
                                                    int questionSentenceIndex,
                                                    Document.Span questionSpan,
                                                    int questionMention) {
        StringBuilder template = new StringBuilder();
        String lastNer = "";
        for (int token = document.getSentence(questionSentenceIndex).getFirstToken();
             token < document.getSentence(questionSentenceIndex).getLastToken();
             ++token) {
            if (token >= questionSpan.getMention(questionMention).getTokenBeginOffset() &&
                    token < questionSpan.getMention(questionMention).getTokenEndOffset()) {
                template.append(" <" + questionSpan.getNerType() + ">");
                token = questionSpan.getMention(questionMention).getTokenEndOffset() - 1;
            }
            else if (!document.getToken(token).getNer().equals("O")) {
                if (!document.getToken(token).getNer().equals(lastNer)) {
                    lastNer = document.getToken(token).getNer();
                    template.append(" [" + lastNer.toUpperCase() + "]");
                }
            } else {
                if (document.getToken(token).getPos().startsWith("W") ||
                        document.getToken(token).getPos().startsWith("MD")
                        || (Character.isLetterOrDigit(document.getToken(token).getPos().charAt(0)) &&
                        !StopAnalyzer.ENGLISH_STOP_WORDS_SET.contains(document.getToken(token).getLemma()))) {
                    lastNer = "";
                    template.append(" " + document.getToken(token).getLemma().toLowerCase());
                }
            }

        }
        return template;
    }

    public static String getSentencePivot(Document.NlpDocument document, int sentenceIndex, int entityToken) {
        return getSentencePivot(document, sentenceIndex, entityToken, -1);
    }

    public static String getSentencePivot(Document.NlpDocument document, int sentenceIndex, int entityToken1, int entityToken2) {
        final int rootTokenIndex = document.getSentence(sentenceIndex).getDependencyRootToken()
                + document.getSentence(sentenceIndex).getFirstToken() - 1;
        if (rootTokenIndex > 0 && rootTokenIndex < document.getTokenCount() && rootTokenIndex != entityToken1 && rootTokenIndex != entityToken2) {
            return NlpUtils.normalizeStringForMatch(document.getToken(rootTokenIndex).getLemma()) + "/" +
                    document.getToken(rootTokenIndex).getPos();
        }
        return null;
    }

    public static List<String> getQuestionWords(Document.NlpDocument document, int questionSentenceIndex) {
        List<String> res = new ArrayList<>();
        for (int i = document.getSentence(questionSentenceIndex).getFirstToken();
             i < document.getSentence(questionSentenceIndex).getLastToken(); ++i) {
            if (document.getToken(i).getPos().startsWith("W")
                    || document.getToken(i).getPos().startsWith("MD")) {
                if (i + 1 < document.getSentence(questionSentenceIndex).getLastToken()
                        && (document.getToken(i + 1).getPos().startsWith("RB") || document.getToken(i + 1).getPos().startsWith("JJ"))
                        /*&& document.getToken(i + 1).getDependencyGovernor() == i - document.getSentence(questionSentenceIndex).getFirstToken() + 1*/) {
                    res.add(NlpUtils.normalizeStringForMatch(document.getToken(i).getLemma()) + " " + NlpUtils.normalizeStringForMatch(document.getToken(i + 1).getLemma()));
                } else {
                    res.add(NlpUtils.normalizeStringForMatch(document.getToken(i).getLemma()));
                }
            }
        }
        return res;
    }
}
