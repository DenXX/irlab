package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by dsavenk on 10/17/14.
 */
public class TestProcessor extends Processor {

    Map<String, Map<String, Integer>> tokenRelationCounts = new HashMap<>();
    Map<String, Integer> relationCounts = new HashMap<>();

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs
     */
    public TestProcessor(Properties properties) throws IOException {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {

        DocumentWrapper doc = new DocumentWrapper(document);
        for (int sent = doc.getQuestionSentenceCount(); sent < document.getSentenceCount(); ++sent) {
            Document.Sentence sentence = document.getSentence(sent);
            if (sentence.getText().endsWith("?")) {
                System.out.println(sentence.getText());
            }
        }

        // Collection relation statistics for text2kb. See finalize method as well.
//        DocumentWrapper doc = new DocumentWrapper(document);
//        int questionSentences = doc.getQuestionSentenceCount();
//        if (document.getRelationCount() > 0) {
//            for (int sentenceIndex = 0; sentenceIndex < questionSentences; ++sentenceIndex) {
//                Document.Sentence sent = document.getSentence(sentenceIndex);
//                for (int tokenIndex = sent.getFirstToken();
//                     tokenIndex < sent.getLastToken(); ++tokenIndex) {
//                    String token = document.getToken(tokenIndex)
//                            .getText().toLowerCase();
//                    if (!Character.isLetterOrDigit(token.charAt(0))) continue;
//
//                    if (!tokenRelationCounts.containsKey(token)) {
//                        tokenRelationCounts.put(token, new HashMap<>());
//                        tokenRelationCounts.get(token).put("*", 0);
//                    }
//                    for (Document.Relation rel : document.getRelationList()) {
//                        String relation = rel.getRelation();
//                        Integer oldCount = tokenRelationCounts.get(token).getOrDefault(relation, 0);
//                        tokenRelationCounts.get(token).put(relation, oldCount + 1);
//                        tokenRelationCounts.get(token).put("*", tokenRelationCounts.get(token).get("*") + 1);
//
//                        Integer relOldCount = relationCounts.getOrDefault(relation, 0);
//                        relationCounts.put(relation, relOldCount + 1);
//                        relOldCount = relationCounts.getOrDefault("*", 0);
//                        relationCounts.put("*", relOldCount + 1);
//                    }
//                }
//            }
//            return document;
//        }
//        return null;


//        Set<Pair<Double, String>> mids = new HashSet<>();
//        for (Document.Span span : document.getSpanList()) {
//            for (int i = 0; i < span.getCandidateEntityIdCount(); ++i) {
//                mids.add(new Pair<>(span.getCandidateEntityScore(i), span.getCandidateEntityId(i)));
//            }
//        }

//        System.out.println(document.getSentence(0).getText());
//        mids.stream()
//                .sorted((e1, e2) -> e2.first.compareTo(e1.first))
//                .limit(5)
//                .forEach(e -> System.out.println(e.second + "\t" + kb_.getEntityName(e.second)));



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
        return null;
    }

    @Override
    public void finishProcessing() {
//        try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("token_relation.txt")))) {
//            Integer totalCount = relationCounts.get("*");
//            for (String token : tokenRelationCounts.keySet()) {
//                Integer tokenCount = tokenRelationCounts.get(token).get("*");
//                for (String relation : tokenRelationCounts.get(token).keySet()) {
//                    if (relation.equals("*")) continue;
//                    Integer tokenRelationCount = tokenRelationCounts.get(token).get(relation);
//                    Integer relationCount = relationCounts.get(relation);
//                    double pmiScore = Math.log(1.0 * tokenRelationCount / tokenCount) - Math.log(1.0 * relationCount / totalCount);
//                    out.write(String.format("%s\t%s\t%d\t%d\t%d\t%d\t",
//                            token, relation, tokenRelationCount, tokenCount,
//                            relationCount, totalCount));
//                    out.write(Double.toString(pmiScore));
//                    out.newLine();
//                }
//            }
//        } catch (Exception ex) {
//            ex.printStackTrace();
//        }

//        System.out.println(tokenRelationCounts.size());
//        Map<String, Map<String, Double>> pmi = new HashMap<>();
//
//        for (Map.Entry<String, Map<String, Integer>> tokenRelations : tokenRelationCounts.entrySet()) {
//            pmi.put(tokenRelations.getKey(), new HashMap<>());
//            for (Map.Entry<String, Integer> relCounts : tokenRelations.getValue().entrySet()) {
//                if (relCounts.getKey().equals("*")) continue;
//                double pmiScore = Math.log(1.0 * relCounts.getValue() / tokenRelations.getValue().get("*"))
//                        - Math.log(1.0 * relationCounts.get(relCounts.getKey()) / relationCounts.get("*"));
//                pmi.get(tokenRelations.getKey()).put(relCounts.getKey(), pmiScore);
//            }
//        }
//        System.out.println(pmi.size());
    }
}
