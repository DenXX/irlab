package edu.emory.mathcs.ir.cqa_dialog.utils;

import edu.stanford.nlp.ling.HasIndex;
import edu.stanford.nlp.ling.HasLemma;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.trees.Constituent;
import edu.stanford.nlp.trees.SemanticHeadFinder;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeLemmatizer;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 2/10/16.
 */
public class NlpUtils {
    private static final Set<String> _stopwords = new HashSet<>(Arrays.asList(
            new String[]{"i", "there", "it", "you", "they", "we", "my", "your",
                    "their", "a", "the", "an", "'s", "'m", "our", "there",
                    "something", "someone", "our", "this", "that", "me", "he",
                    "she", "anything", "these", "some", "itself", "everything"}));
    private static final TreeLemmatizer _treeLemmatizer = new TreeLemmatizer();
    private static final SemanticHeadFinder _headFinder = new SemanticHeadFinder();
    private static final Properties _properties = new Properties();

    static {
        _properties.setProperty("parse.model",
                "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
    }

    public static String[] extractNp(Document doc, boolean includeNested) {
        return doc.sentences()
                .stream()
                .flatMap(sent -> Arrays.stream(NlpUtils.extractNp(sent, includeNested)))
                .toArray(String[]::new);
    }

    public static String[] extractNp(Sentence sentence, boolean includeNested) {
        Tree parseTree = sentence.parse(_properties);
        List<String> nps = new ArrayList<>();
        for (Tree subtree : parseTree) {
            if (subtree.isPhrasal() &&
                    subtree.label().value().equals("NP")) {

                if (!includeNested) {
                    boolean hasNested = Arrays.stream(subtree.children())
                            .anyMatch(tree -> tree.isPhrasal() &&
                                    tree.label().value().equals("NP"));
                    if (hasNested) {
                        continue;
                    }
                }

                subtree = _treeLemmatizer.transformTree(subtree);

                String phrase = subtree.getLeaves()
                        .stream()
                        .map(leaf -> ((HasLemma) leaf.label()).lemma().toLowerCase().trim().isEmpty() ?
                                ((HasWord)leaf.label()).word().trim().toLowerCase() :
                                ((HasLemma) leaf.label()).lemma().trim().toLowerCase())
                        .filter(term -> !_stopwords.contains(term) &&
                                !term.isEmpty())
                        .collect(Collectors.joining(" "));

                // Get np phrase.
                if (!phrase.isEmpty()) {
                    nps.add(phrase);
                }
            }
        }
        return nps.toArray(new String[nps.size()]);
    }
}
