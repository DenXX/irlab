package edu.emory.mathcs.ir.cqa_dialog.utils;


import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.util.CoreMap;

import java.io.*;
import java.util.Properties;

/**
 * Created by dsavenk on 12/10/15.
 */
public class ParseQuestions {
    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize, ssplit, pos, lemma, depparse");
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
                }
                if (index++ % 1000 == 0) {
                    System.err.println(String.format("%d questions parsed.", index));
                }
            }
        }
    }
}
