package edu.emory.mathcs.ir.cqa_dialog.utils;


import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;

/**
 * Created by dsavenk on 12/10/15.
 */
public class ParseQuestions {
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
                System.out.println("--------------------------");
                System.out.println(fields[0] + "\t" + fields[1]);
                System.out.println(fields[2]);

                for (CoreLabel token : document.get(CoreAnnotations.TokensAnnotation.class)) {
                    System.out.print(token.originalText() + "/" + token.tag() + "/" + token.lemma() + "\t");
                }
                System.out.println();

                for (CoreMap sentence : document.get(CoreAnnotations.SentencesAnnotation.class)) {
                    SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
                    System.out.println(graph.toString());

                    Tree tree = sentence.get(TreeCoreAnnotations.TreeAnnotation.class);
                    for (Tree subtree : tree) {
                        if (subtree.isPhrasal() &&
                                subtree.label().value().equals("NP")) {
                            for (Tree leaf : subtree.getLeaves()) {
                                System.out.print(" " + leaf.label().value());
                            }
                            System.out.print("\t");
                        }
                    }
                    System.out.println();
                }
                if (index++ % 1000 == 0) {
                    System.err.println(String.format("%d questions parsed.", index));
                }
            }
        }
    }
}
