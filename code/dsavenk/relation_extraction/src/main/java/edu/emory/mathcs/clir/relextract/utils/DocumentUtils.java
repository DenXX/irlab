package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by dsavenk on 1/29/15.
 */
public class DocumentUtils {

    public static int getQuestionSentenceCount(Document.NlpDocument document) {
        for (int i = 0; i < document.getSentenceCount(); ++i) {
            if (document.getToken(document.getSentence(i).getFirstToken()).getBeginCharOffset() >= document.getQuestionLength())
                return i;
        }
        return document.getSentenceCount();
    }

    public static List<String> getQuestionLemmas(Document.NlpDocument document) {
        List<String> questionLemma = new ArrayList<>();
        for (int i = 0; i < document.getTokenCount(); ++i) {
            if (document.getToken(i).getBeginCharOffset() >= document.getQuestionLength()) {
                return questionLemma;
            }
            questionLemma.add(NlpUtils.normalizeStringForMatch(document.getToken(i).getLemma()));
        }
        return questionLemma;
    }
}
