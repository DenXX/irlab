package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

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
        System.out.println("---------------------------");
        System.out.println(document.getText());
        for (Document.Span span : document.getSpanList()) {
            if ("ENTITY".equals(span.getType()) ||
                    "OTHER".equals(span.getType())) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getMentionType().equals("NOMINAL") ||
                            mention.getMentionType().equals("PROPER")) {
                        System.out.println(fixName(mention.getValue()) + " - " + (mention.hasEntityId() ? mention.getEntityId() : ""));
                    }
                }
            }
        }
//        ++total;
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
        return null;
    }

    @Override
    public void finishProcessing() {
//        System.out.println("With relations: " + count + "\nTotal: " + total);
    }
}
