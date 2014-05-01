package edu.emory.cqaqa.utils;

import edu.emory.cqaqa.tools.Freebase;
import edu.emory.cqaqa.types.FreebaseEntityAnnotation;
import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;
import edu.stanford.nlp.util.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 4/30/14.
 */
public class NlpUtils {
    private static AbstractSequenceClassifier<CoreLabel> nerTagger = null;
    private static Map<String, Pair<String, Long>> lexicon = null;

    static {
        {
            try {
                nerTagger = CRFClassifier.getClassifier("edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz");
            } catch (IOException e) {   // TODO: Need some better way to do this.
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        {
            try {
                lexicon = new HashMap<String, Pair<String, Long>>();
                BufferedReader reader = new BufferedReader(new InputStreamReader(
                        new FileInputStream("/home/dsavenk/ir/lexicon.txt")));
                String line;
                while ((line = reader.readLine()) != null) {
                    String[] fields = line.split("\t");
                    for (int i = 2; i < fields.length; i += 2) {
                        long count = Long.parseLong(fields[i]);
                        String phrase = fields[i-1].toLowerCase();
                        if (!lexicon.containsKey(phrase) || lexicon.get(phrase).second < count) {
                            lexicon.put(phrase, new Pair<String, Long>(fields[0], count));
                        }
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static List<List<CoreLabel>> detectEntities(String text) {
        List<List<CoreLabel>> sentences = nerTagger.classify(text);
        List<List<CoreLabel>> res = new ArrayList<List<CoreLabel>>();
        for (List<CoreLabel> sentence : sentences) {
            List<CoreLabel> resSentence = new ArrayList<CoreLabel>();
            for (CoreLabel word : sentence) {
                word.setNER(word.get(CoreAnnotations.AnswerAnnotation.class));
                if (!word.ner().equals("O") && resSentence.size() > 0 &&
                        resSentence.get(resSentence.size() - 1).ner().equals(word.ner())) {
                    CoreLabel prev = resSentence.get(resSentence.size() - 1);
                    prev.setEndPosition(word.endPosition());
                    prev.setAfter(word.after());
                    prev.setWord(prev.word() + " " + word.word());
                    prev.setOriginalText(prev.originalText() + " " + word.originalText());
                    prev.setValue(prev.value() + " " + word.value());
                } else {
                    word.setSentIndex(resSentence.size());
                    resSentence.add(word);
                }
            }
            res.add(resSentence);
        }
        return res;
    }

    public static void linkToFreebase(List<List<CoreLabel>> sentences) {
        for (List<CoreLabel> sentence : sentences) {
            for (CoreLabel token : sentence) {
                if (token.ner().equals("ORGANIZATION") ||
                    token.ner().equals("PERSON") ||
                    token.ner().equals("ORGANIZATION")) {

                    String normalizedPhrase = token.word().toLowerCase();
                    if (lexicon.containsKey(normalizedPhrase)) {
                        token.set(FreebaseEntityAnnotation.class, lexicon.get(normalizedPhrase).first);
                    }
                } else {
                    token.set(FreebaseEntityAnnotation.class, null);
                }
            }
        }
    }

}
