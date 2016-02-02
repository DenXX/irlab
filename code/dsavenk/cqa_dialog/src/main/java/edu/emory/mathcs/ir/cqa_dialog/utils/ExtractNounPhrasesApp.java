package edu.emory.mathcs.ir.cqa_dialog.utils;

import edu.stanford.nlp.ie.machinereading.structure.Span;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasLemma;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeLemmatizer;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 1/25/16.
 */
public class ExtractNounPhrasesApp {

    private final static Set<String> STOPWORDS = new HashSet<>(Arrays.asList(
            new String[]{"i", "there", "it", "you", "they", "we", "my", "your",
                    "their", "a", "the", "an", "'s", "'m", "our", "there",
                    "something", "someone", "our", "this", "that", "me", "he",
                    "she", "anything", "these", "some", "itself", "everything"}));

    private static final SemanticHeadFinder headFinder = new SemanticHeadFinder();
    private static final TreeLemmatizer treeLemmatizer = new TreeLemmatizer();

    private static String cleanUp(String line) {
        return line.replaceAll("<[^>]*>", " ");
    }

    private static void printNp(String np, String head) {
        np = np.trim();
        if (!np.isEmpty()) {
            System.out.println(np.replaceAll("^(a[n]?|the)\\s", "") + "\t" + head);
        }
    }

    private static void extractNp(String line) {
        Document doc = new Document(line);
        for (Sentence sent : doc.sentences()) {
            Properties props = new Properties();
            props.setProperty("parse.model", "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
            Tree parseTree = sent.parse(props);
                for (Tree subtree : parseTree) {
                    if (subtree.isPhrasal() &&
                            subtree.label().value().equals("NP")) {
                        subtree = treeLemmatizer.transformTree(subtree);

                        // Get np phrase.
                        String np = subtree.getLeaves()
                                .stream()
                                .map(leaf -> ((HasLemma)leaf.label()).lemma().toLowerCase())
                                .filter(term -> !STOPWORDS.contains(term))
                                .collect(Collectors.joining(" "));

                        String head =
                                ((HasLemma)subtree.headTerminal(headFinder).label()).lemma().toLowerCase();

                        printNp(np, head);

//                        if (np.contains(" or ") || np.contains(" and ")) {
//                            String[] piecesStr = np.split("\\s(and|or)\\s");
//                            for (String pieceStr : piecesStr) {
//                                printNp(pieceStr);
//                            }
//                        } else {
//                            printNp(np);
//                        }
                    }
                }
        }
    }

    public static void main(String... args) {
        File qaTextFile = new File(args[0]);
        try (BufferedReader reader =
                     new BufferedReader(new FileReader(qaTextFile))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                if (!line.isEmpty()) {
                    extractNp(cleanUp(line));
                }
                if (index % 100 == 0) {
                    System.err.println(index + " lines processed.");
                }
                ++index;
            }
        } catch (IOException e) {
            e.printStackTrace();
        } ;

    }
}
