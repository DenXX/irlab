package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.QuestionAnswerPair;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 4/30/14.
 */
public class LinkPredicateProcessor implements QuestionAnswerPairProcessor {
    MultiKeyMap<String, String> triples = new MultiKeyMap<String, String>();

    public LinkPredicateProcessor(String rdfFilename) throws IOException {
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(new FileInputStream(rdfFilename))));
        String line;
        System.err.println("Reading RDF...");
        while ((line = reader.readLine()) != null) {
            String[] triple = line.split("\t");
            String objectMid = stripRdf(triple[0]);
            String subjectMid = stripRdf(triple[2]);
            String predicate = stripRdf(triple[1]);
            if (objectMid.contains("/m/") && subjectMid.contains("/m/")) {
                triples.put(objectMid, subjectMid, predicate);
            }
        }
        reader.close();
        System.err.println("Finished reading RDF.");
    }

    public static String stripRdf(String rdfUri) {
        return rdfUri.replace("<http://rdf.freebase.com/ns", "").replace(">", "").replace(".", "/");
    }

    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair qa) {
        if (triples.containsKey(qa.getAttribute("question_entity"), qa.getAttribute("answer_entity"))) {
            qa.addAttribute("predicate",
                    triples.get(qa.getAttribute("question_entity"), qa.getAttribute("answer_entity")));
        } else if (triples.containsKey(qa.getAttribute("answer_entity"), qa.getAttribute("question_entity"))) {
            qa.addAttribute("predicate",
                    triples.get(qa.getAttribute("answer_entity"), qa.getAttribute("question_entity")));
        }
        return qa;
    }

}
