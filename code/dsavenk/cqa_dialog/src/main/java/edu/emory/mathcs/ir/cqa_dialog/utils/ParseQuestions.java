package edu.emory.mathcs.ir.cqa_dialog.utils;


import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.tokensregex.Env;
import edu.stanford.nlp.ling.tokensregex.NodePattern;
import edu.stanford.nlp.ling.tokensregex.TokenSequenceMatcher;
import edu.stanford.nlp.ling.tokensregex.TokenSequencePattern;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 12/10/15.
 */
public class ParseQuestions {

    private static boolean extractTemplate(Tree tree, Set<String> nps) {
        boolean isNp = tree.isPhrasal() && tree.label().value().equals("NP");
        boolean hasChildNp = false;
        for (Tree subtree : tree.children()) {
            hasChildNp |= extractTemplate(subtree, nps);
        }
        if (isNp && !hasChildNp) {
            // Get np phrase.
            String np = tree.getLeaves()
                    .stream()
                    .map(leaf -> leaf.label().value())
                    .collect(Collectors.joining(" "));
            if (!np.toLowerCase().equals("you")) {
                nps.add(np);
            }
        }
        return isNp | hasChildNp;
    }

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse, parse");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(args[0])))) {
            String line;
            int index = 0;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t");
                Annotation document = new Annotation(fields[2]);
                pipeline.annotate(document);
//                System.out.println("--------------------------");
//                System.out.println(fields[0] + "\t" + fields[1]);
//                System.out.println(fields[2]);
                String question = fields[2];



//                for (CoreLabel token : document.get(CoreAnnotations.TokensAnnotation.class)) {
//                    System.out.print(token.originalText() + "/" + token.tag() + "/" + token.lemma() + "\t");
//                }
//                System.out.println();

                for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
                    List<CoreLabel> sentenceTokens = sentence.get(CoreAnnotations.TokensAnnotation.class);

//                    SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
//                    System.out.println(graph.toString());

                    Env env = TokenSequencePattern.getNewEnv();
                    env.setDefaultStringMatchFlags(NodePattern.CASE_INSENSITIVE);
                    env.setDefaultStringPatternFlags(Pattern.CASE_INSENSITIVE);
                    TokenSequencePattern pattern = TokenSequencePattern.compile(env,
                            "([ { word:/what|which/ } ] []{0,2} [ { word:/type|kind/ } ] [ { word:\"of\" } ]) | " +
                            "([ { word:\"how\" } ] []{0, 2} [ { word:/is|are/ } ]) | " +
                            "([ { word:/is|are/ } ] [ { word:/it|this|these/ } ] [ { word:\"a\" } ])");
                    TokenSequenceMatcher matcher = pattern.getMatcher(sentenceTokens);

                    List<List<CoreMap>> matches = new ArrayList<>();
                    List<String> matchesStr = new ArrayList<>();
                    while (matcher.find()) {
                        String matchedString = matcher.group();
                        List<CoreMap> matchedTokens = matcher.groupNodes();
                        matches.add(matchedTokens);
                        matchesStr.add(matchedString);
                    }

                    Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);

                    Set<String> nps = new HashSet<>();
                    extractTemplate(tree, nps);
                    if (!nps.isEmpty()) {
                        String patternString = String.join("|", nps)
                                .replace("?", "\\?")
                                .replace("+", "\\+")
                                .replace("^", "\\^")
                                .replace("!", "\\!")
                                .replace("[", "\\[")
                                .replace("]", "\\]")
                                .replace("(", "\\(")
                                .replace(")", "\\)")
                                .replace("{", "\\{")
                                .replace("}", "\\}")
                                .replace("\\", "\\\\")
                                .replace("*", "\\*");
                        Pattern ptrn = Pattern.compile(patternString);
                        Matcher mtch = ptrn.matcher(question);

                        StringBuffer sb = new StringBuffer();
                        while (mtch.find()) {
                            mtch.appendReplacement(sb, "<NP>");
                        }
                        mtch.appendTail(sb);

                        mtch = ptrn.matcher(question);
                        StringBuffer sb2 = new StringBuffer();
                        while (mtch.find()) {
                            String str = mtch.group();
                            mtch.appendReplacement(sb2, "<" + str + ">");
                        }
                        mtch.appendTail(sb2);

                        System.out.println(sb.toString() + "\t" + sb2.toString());
                    }

                    Set<String> orParts = new HashSet<>();
//                    for (Tree subtree : tree) {
//                        if (subtree.isPhrasal() &&
//                                subtree.label().value().equals("NP")) {
//
//                            // Get np phrase.
//                            String np = subtree.getLeaves()
//                                    .stream()
//                                    .map(leaf -> leaf.label().value())
//                                    .collect(Collectors.joining(" "));
//
//                            if (np.contains(" or ")) {
//                                Collections.addAll(orParts,
//                                        np.split("\\sor\\s"));
//                            } else {
//                                System.out.print(np + "\t");
//                                int npBeginning = ((CoreLabel) subtree.label()).get(CoreAnnotations.BeginIndexAnnotation.class);
//
//                                int i = 0;
//                                for (List<CoreMap> match : matches) {
//                                    int beginMatch = match.get(match.size() - 1).get(CoreAnnotations.BeginIndexAnnotation.class);
//                                    int endMatch = match.get(match.size() - 1).get(CoreAnnotations.EndIndexAnnotation.class);
//                                    if (npBeginning - beginMatch >= 0 &&
//                                            (npBeginning - endMatch <= 1 || orParts.contains(np))) {
//                                        System.out.print(matchesStr.get(i));
//                                        System.out.print("\t");
//                                    }
//                                    ++i;
//                                }
//
//                                System.out.println();
//                            }
//                        }
//                    }
                }
                if (index++ % 1000 == 0) {
                    System.err.println(String.format("%d questions parsed.", index));
                }
            }
        }
    }
}
