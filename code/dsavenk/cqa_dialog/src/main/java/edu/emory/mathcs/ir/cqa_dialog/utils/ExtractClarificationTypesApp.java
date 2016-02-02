package edu.emory.mathcs.ir.cqa_dialog.utils;

import edu.stanford.nlp.ling.HasIndex;
import edu.stanford.nlp.ling.HasLemma;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.SentenceAlgorithms;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeLemmatizer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 1/25/16.
 */
public class ExtractClarificationTypesApp {

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
            parseTree.indexLeaves();

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

                    if (!np.isEmpty()) {
                        Tree head = subtree.headTerminal(headFinder);

                        List<String> path = sent.algorithms().dependencyPathBetween(0,
                                ((HasIndex) head.label()).index() - 1, Sentence::lemmas);
                        List<String> filteredPath = new ArrayList<>();
                        for (int i = 0; i < path.size(); i += 2) {
                            filteredPath.add(path.get(i));
                        }
                        if (filteredPath.size() > 0) {
                            filteredPath.remove(filteredPath.size() - 1);
                        }
                        List<String> keyphrases = sent.algorithms().keyphrases();

                        String headStr =
                                ((HasLemma) subtree.headTerminal(headFinder).label()).lemma().toLowerCase();

                        String parent = filteredPath.size() > 0 ? filteredPath.get(filteredPath.size() - 1) : "";

                        System.out.println(np + "\t" + headStr + "\t" + String.join(" ", filteredPath) + "\t" + parent + "\t" + String.join(",", keyphrases));
                    }
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
