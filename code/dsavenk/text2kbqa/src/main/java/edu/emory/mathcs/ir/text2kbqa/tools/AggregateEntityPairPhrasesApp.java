package edu.emory.mathcs.ir.text2kbqa.tools;

import edu.emory.mathcs.ir.text2kbqa.util.Stopwords;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.text.Normalizer;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;
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
            String[] currentEntityPair = new String[2];
            Map<String, Integer> tokenCounts = new HashMap<>();
            String line;
            while ((line = input.readLine()) != null) {
                String[] fields = line.split("\t");
                String firstMid = fields[0];
                String firstName = fields[1];
                String secondMid = fields[2];
                String secondName = fields[3];
                String phrase = fields[4];

                if (!firstMid.equals(currentEntityPair[0]) ||
                        !secondMid.equals(currentEntityPair[1])) {
                    writeEntityPairTokens(out, tokenCounts, firstMid, firstName,
                            secondMid, secondName);
                    tokenCounts.clear();
                    currentEntityPair[0] = firstMid;
                    currentEntityPair[1] = secondMid;
                }

                for (String token :
                        getTokens(phrase, firstName, secondName, nlp)) {
                    Integer oldCount = tokenCounts.putIfAbsent(token, 0);
                    tokenCounts.put(token, oldCount == null ? 1 : oldCount + 1);
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void writeEntityPairTokens(BufferedWriter out,
                                              Map<String, Integer> tokenCounts,
                                              String firstMid, String firstName,
                                              String secondMid,
                                              String secondName)
            throws IOException {
        out.write(String.format("%s\t%s\t%s\t%s\t",
                firstMid, firstName, secondMid, secondName));
        String tokensStr =
                tokenCounts.entrySet()
                .stream()
                .sorted((e1, e2) -> e2.getValue().compareTo(e1.getValue()))
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining("\t"));
        out.write(tokensStr);
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
            tokens.addAll(sentence.get(
                    CoreAnnotations.TokensAnnotation.class).stream()
                    .map(token -> token.word().toLowerCase())
                    .filter(token -> !token.isEmpty() &&
                            Character.isLetterOrDigit(token.charAt(0)) &&
                            !Stopwords.isStopword(token))
                    .collect(Collectors.toList()));
        }
        return tokens.toArray(new String[tokens.size()]);
    }
}
