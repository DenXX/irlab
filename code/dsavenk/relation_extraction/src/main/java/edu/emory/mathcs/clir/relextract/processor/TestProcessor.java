package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by dsavenk on 10/17/14.
 */
public class TestProcessor extends Processor {
    private int count = 0;
    private int total = 0;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public TestProcessor(Properties properties) throws IOException {
        super(properties);
    }

    private static String fixName(String name) {
        return name.replace("-LRB- ", "(")
                .replace(" -RRB-", ")")
                .replace(" ,", ",")
                .replace(" .", ".")
                .replace(" !", "!")
                .replace(" ?", "?")
                .replace(" :", ":")
                .replace("` ", "`")
                .replace(" '", "'");
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (document.getText().contains("What is the difference between a cold and the flu")) {
            return document;
        }
        return null;


//        int i = 0;
//        ++total;
//        while (i < document.getSentenceCount()) {
//            if (document.getToken(document.getSentence(i).getFirstToken()).getBeginCharOffset() >= document.getQuestionLength()) {
//                break;
//            }
//            ++i;
//        }
//        if (i == document.getSentenceCount() - 1 &&
//                document.getSentence(i).getLastToken() - document.getSentence(i).getFirstToken() <= 5) {
//            System.out.println(document.getText());
//            System.out.println(document.getRelationCount());
//            ++count;
//            return document;
//        }

        // --------------------

//
//        int questionSentencesCount = 0;
//        for (Document.Sentence sent : document.getSentenceList()) {
//            if (document.getToken(sent.getFirstToken()).getBeginCharOffset() >=
//                    document.getQuestionLength()) {
//                break;
//            }
//            ++questionSentencesCount;
//        }
//        if (questionSentencesCount != 1) return null;
//
//        if (!document.getToken(0).getText().toLowerCase().equals("how"))
//            return null;
//
//        System.out.println(document.getSentence(0).getText().replace("\n", " "));


//        for (Document.Span span : document.getSpanList()) {
//            if (span.getType().equals("ENTITY")) {
//                System.out.println(span.getValue() + "\t" + (span.hasEntityId() ? span.getEntityId() : ""));
//            }
//        }
//        if (document.getRelationCount() == 0) {
//            int count = 0;
//            for (Document.Span span : document.getSpanList()) {
//                if (span.hasEntityId()) {
//                    ++count;
//                }
//            }
//            if (count >= 2) {
//                System.out.println("-------------------------------------");
//                System.out.println(document.getText());
//                System.out.println("===");
//                for (Document.Span span : document.getSpanList()) {
//                    if (span.hasEntityId()) {
//                        System.out.println(span.getText() + " [" + span.getEntityId() + "]");
//                    }
//                }
//            }
//        }
//        return null;
    }

    @Override
    public void finishProcessing() {
        System.out.println("Single phrase questions: " + count);
        System.out.println("Total: " + total);
    }
}
