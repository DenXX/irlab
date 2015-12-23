package edu.emory.mathcs.ir.text2kbqa.tools;

import edu.emory.mathcs.ir.text2kbqa.util.Stopwords;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.util.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 12/17/15.
 */
public class AggregateEntityPairPhrasesApp {
    public static void main(String[] args) {
        final StanfordCoreNLP nlp = getNlpAnnotator();

        try (BufferedReader input =
                     new BufferedReader(
                             new InputStreamReader(
                                     new GZIPInputStream(
                                             new FileInputStream(args[0]))));
             BufferedWriter out =
                     new BufferedWriter(
                             new OutputStreamWriter(System.out))
        ) {
            String[] currentEntityPairMids = new String[2];
            String[] currentEntityPairNames = new String[2];
            Map<String, Integer> tokenCounts = new HashMap<>();
            String line;
            while ((line = input.readLine()) != null) {
                String[] fields = line.split("\t");
                String firstMid = fields[0];
                String firstName = fields[1];
                String secondMid = fields[2];
                String secondName = fields[3];
                String phrase = fields[4];

                if (!firstMid.equals(currentEntityPairMids[0]) ||
                        !secondMid.equals(currentEntityPairMids[1])) {
                    writeEntityPairTokens(out, tokenCounts,
                            currentEntityPairMids[0], currentEntityPairNames[0],
                            currentEntityPairMids[1], currentEntityPairNames[1]);
                    tokenCounts.clear();
                    currentEntityPairMids[0] = firstMid;
                    currentEntityPairMids[1] = secondMid;
                    currentEntityPairNames[0] = firstName;
                    currentEntityPairNames[1] = secondName;
                }

                for (String token :
                        getTokens(phrase, firstName, secondName, nlp)) {
                    if (!tokenCounts.containsKey(token)) {
                        tokenCounts.put(token, 0);
                    }
                    Integer oldCount = tokenCounts.get(token);
                    tokenCounts.put(token, oldCount + 1);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void writeEntityPairTokens(BufferedWriter out,
                                              Map<String, Integer> tokenCounts,
                                              String firstMid,
                                              String firstName,
                                              String secondMid,
                                              String secondName)
            throws IOException {
        if (firstMid == null) return;

        out.write(String.format("%s\t%s\t%s\t%s\t",
                firstMid, firstName, secondMid, secondName));

        List<Map.Entry<String, Integer>> entries = new ArrayList<>(tokenCounts.entrySet());

        Collections.sort(entries, new Comparator<Map.Entry<String, Integer>>() {
            @Override
            public int compare(Map.Entry<String, Integer> e1, Map.Entry<String, Integer> e2) {
                return e2.getValue().compareTo(e1.getValue());
            }
        });

        for (Map.Entry<String, Integer> entry: entries) {
            out.write(entry.getKey() + ":" + entry.getValue() + "\t");
        }
        out.newLine();
    }

    private static StanfordCoreNLP getNlpAnnotator() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit");
        return new StanfordCoreNLP(props);
    }

    private static String[] getTokens(String phrase,
                                      String firstEntityName,
                                      String secondEntityName,
                                      StanfordCoreNLP nlp) {
        List<String> tokens = new ArrayList<>();
//        phrase = phrase
//                .replace(firstEntityName, "ENTITY1")
//                .replace(secondEntityName, "ENTITY2");
        Annotation phraseAnnotation = new Annotation(phrase);
        nlp.annotate(phraseAnnotation);
        for (CoreMap sentence : phraseAnnotation.get(
                CoreAnnotations.SentencesAnnotation.class)) {
            for (CoreLabel token : sentence.get(CoreAnnotations.TokensAnnotation.class)) {
                String tokenStr = token.word().toLowerCase();
                if (!tokenStr.isEmpty() &&
                        Character.isLetterOrDigit(tokenStr.charAt(0)) &&
                        !Stopwords.isStopword(tokenStr)) {
                    tokens.add(tokenStr);
                }
            }
        }
        return tokens.toArray(new String[tokens.size()]);
    }
}
