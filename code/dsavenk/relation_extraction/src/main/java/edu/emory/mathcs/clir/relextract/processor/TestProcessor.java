package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;
import java.util.Random;

/**
 * Created by dsavenk on 10/17/14.
 */
public class TestProcessor extends Processor {
    private int count = 0;
    private int count2 = 0;
    private int total = 0;

    private boolean flag = false;

    private final Random rnd;

    private final KnowledgeBase kb_;

    BufferedWriter out_ = new BufferedWriter(new OutputStreamWriter(System.out));

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs
     */
    public TestProcessor(Properties properties) throws IOException {
        super(properties);
        rnd = new Random(42);
        kb_ = KnowledgeBase.getInstance(properties);
        flag = properties.containsKey(QAModelTrainerProcessor.QA_DEBUG_PARAMETER);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        //document.getQaInstanceList().stream().filter(Document.QaRelationInstance::getIsPositive).forEach(System.out::println);
        ++total;
        boolean first = true;
        StringBuilder res = new StringBuilder();
        for (Document.QaRelationInstance triple : document.getQaInstanceList()) {
            if ((flag || triple.getIsPositive()) && first) {
                res.append("----------------------------------\n").append(document.getText()).append("\n");
                first = false;
                ++count;
            }
            if (triple.getIsPositive()) {
                res.append(kb_.getEntityName(triple.getSubject()))
                        .append("[")
                        .append(triple.getSubject())
                        .append("]\t")
                        .append(triple.getPredicate())
                        .append("\t")
                        .append(triple.getObject().startsWith("http:")
                            ? (kb_.getEntityName(triple.getObject()) + "[/" + triple.getObject().substring(triple.getObject().lastIndexOf("/") + 1).replace(".", "/") + "]")
                            : triple.getObject())
                        .append("\n");
            }
        }
        if (res.length() > 0) {
            synchronized (this) {
                System.out.println(res.toString());
            }
        }
        return null;

//        for (Document.Span span : document.getSpanList()) {
//            if ("ENTITY".equals(span.getType())) {
//                if (span.getMention(span.getRepresentativeMention()).getType().equals("OTHER"))
//                    return null;
//            }
//        }


//        count += document.getQuestionLength();
//        count2 += (document.getRelationCount() > 0) ? 1 : 0;

//        int questionSents = DocumentUtils.getQuestionSentenceCount(document);
//        if (document.getSentence(0).getText().contains("Who won"))
//            return document;
//        int questionSents = DocumentUtils.getQuestionSentenceCount(document);
//        if (questionSents > 0 && document.getSentenceCount() - questionSents > 0) {
//            boolean verbs = false;
//            for (int i = questionSents; i < document.getSentenceCount(); ++i) {
//                for (int token = document.getSentence(i).getFirstToken(); token < document.getSentence(i).getLastToken(); ++token) {
//                    if (document.getToken(token).getPos().startsWith("V"))
//                        verbs = true;
//                }
//            }
//
//            if (!verbs) {
//                ++count;
//                return document;
//            }
//        }

        //return null;
//
//        return document;
//        ++total;
//        for (Document.Attribute attr : document.getAttributeList()) {
//            if (attr.getKey().equals("qlang")) {
//                if (!attr.getValue().equals("en")) return null;
//            }
//        }
//        int index = 0;
//        for (Document.Sentence sent : document.getSentenceList()) {
//            System.out.print("<DOC:" + total + ">\t<SENT:" + total + "-" + index++ + ">\t");
//            for (int i = sent.getFirstToken(); i < sent.getLastToken(); ++i) {
//                if (Character.isAlphabetic(document.getToken(i).getPos().charAt(0))) {
//                    System.out.print(document.getToken(i).getLemma().replace("\n", " ").replace("\t", " ") + "\t");
//                }
//            }
//            System.out.println();
//        }
//        return document;

//        if (rnd.nextFloat() < 0.001) {
//            String category = "";
//            String subcategory = "";
//            String content = "";
//            for (Document.Attribute attr : document.getAttributeList()) {
//                if (attr.getKey().equals("cat")) {
//                    category = attr.getValue();
//                } else if (attr.getKey().equals("subcat")) {
//                    subcategory = attr.getValue();
//                } else if (attr.getKey().equals("content")) {
//                    content = attr.getValue();
//                }
//            }
//            System.out.println("Category: " + category + "; Subcategory: " + subcategory);
//            System.out.println("Question:");
//            int sentence = 0;
//            while (sentence < document.getSentenceCount() &&
//                    document.getToken(document.getSentence(sentence).getFirstToken()).getBeginCharOffset() < document.getQuestionLength()) {
//                System.out.println("\t" + document.getSentence(sentence++).getText().replace("\n", "\n\t"));
//            }
//            if (!content.isEmpty()) {
//                System.out.println("Content: ");
//                System.out.println("\t" + content);
//            }
//            System.out.println("Answer:");
//            while (sentence < document.getSentenceCount()) {
//                System.out.println("\t" + document.getSentence(sentence++).getText().replace("\n", "\n\t"));
//            }
//            System.out.println("--------------------------------------------");
//            return document;
//        }
//        return null;


//        document.getSpanList().stream().filter(span -> span.hasEntityId() || (span.hasNerType() && span.getNerType().equals("DATE"))).forEach(span -> {
//            ++count;
//        });
//        ++total;
//        return null;
//        if (document.getText().contains("The first one you're looking for may be The Man Who Lost His Head by Claire Huchet Bishop and Robert McCloskey.")) {
//            ++count;
//            return document;
//        }
//        return null;

//        String lang = "";
//        for (Document.Attribute attr : document.getAttributeList()) {
//            if (attr.getKey().equals("qlang")) lang = attr.getValue();
//        }
//        if (!lang.equals("en")) return null;
//        int questionSentencesCount = 0;
//        while (questionSentencesCount < document.getSentenceCount()) {
//            if (document.getToken(document.getSentence(questionSentencesCount).getFirstToken()).getBeginCharOffset() >= document.getQuestionLength()) {
//                break;
//            }
//            ++questionSentencesCount;
//        }
//        if (questionSentencesCount == 1) {
//            int inQuestionCount = 0;
//            for (Document.Span span : document.getSpanList()) {
//                if (!span.hasEntityId() ||
//                        span.getType().equals("OTHER")) continue;
//                boolean inQuestion = false;
//                for (Document.Mention mention : span.getMentionList()) {
//                    if (mention.getSentenceIndex() == 0) {
//                        inQuestion = true;
//                    }
//                }
//                if (inQuestion) ++inQuestionCount;
//            }
//            if (inQuestionCount == 1) {
//                List<Integer> qWords = new ArrayList<>();
//                for (int token = document.getSentence(0).getFirstToken();
//                        token < document.getSentence(0).getLastToken(); ++token) {
//                    if (document.getToken(token).getPos().startsWith("W")) {
//                        qWords.add(token);
//                    }
//                }
//                if (qWords.size() > 0) {
//                    for (int qWord : qWords) {
//                        int token = document.getSentence(0).getFirstToken() + document.getToken(qWord).getDependencyGovernor() - 1;
//                        if (token >= 0) {
//                            System.out.println(document.getSentence(0).getText().replace("\t", " ").replace("\n", " ") + "\t" + NlpUtils.normalizeStringForMatch(document.getToken(qWord).getLemma()) + "\t" + NlpUtils.normalizeStringForMatch(document.getToken(token).getLemma()) + "\t" + document.getToken(qWord).getDependencyType());
//                        }
//                    }
//                }
//
//                return document;
//            }
//        }
//        return null;

//        int i = 0;
//        ++total;
//        while (i < document.getSentenceCount()) {
//            if (document.getToken(document.getSentence(i).getFirstToken()).getBeginCharOffset() >= document.getQuestionLength()) {
//                break;
//            }
//            ++i;
//        }
//        if (i == 1 && document.getSentenceCount() == 2) {
//            ++count;
//            System.out.println(document.getText());
//            System.out.println("-------------------------------------------");
//            return document;
//        }
//        return null;

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
        System.out.println("Resolved entities: " + count);
        System.out.println("Total: " + total);
        System.out.println("Entities per document: " + 1.0 * count / total);
        System.out.println("QnA with answer without verbs: " + count2);
    }
}
