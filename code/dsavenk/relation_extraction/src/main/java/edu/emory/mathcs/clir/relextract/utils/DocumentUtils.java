package edu.emory.mathcs.clir.relextract.utils;

import edu.emory.mathcs.clir.relextract.data.Document;

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
}
