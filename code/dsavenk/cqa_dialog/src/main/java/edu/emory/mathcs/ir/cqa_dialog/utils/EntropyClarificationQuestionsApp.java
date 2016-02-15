package edu.emory.mathcs.ir.cqa_dialog.utils;

import edu.emory.mathcs.ir.cqa_dialog.Question;
import edu.emory.mathcs.ir.cqa_dialog.QuestionArchive;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;
import org.xml.sax.SAXException;


import javax.xml.parsers.ParserConfigurationException;
import java.io.*;
import java.util.*;

/**
 * Created by dsavenk on 2/10/16.
 */
public class EntropyClarificationQuestionsApp {
    public static void main(String... args) {
        Question[] questions = new Question[0];
        try (InputStream postsStream = new BufferedInputStream(
                new FileInputStream(args[0]));
             InputStream commentsStream = new BufferedInputStream(
                     new FileInputStream(args[1]))) {
            questions = QuestionArchive.readQuestions(postsStream, commentsStream);

        } catch (IOException | ParserConfigurationException | SAXException e) {
            e.printStackTrace();
        }

        Map<String, Double> entropy = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[2])))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t");
                entropy.put(fields[0], Double.parseDouble(fields[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Integer> freq = new HashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(args[3])))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] fields = line.split("\t");
                freq.put(fields[0], Integer.parseInt(fields[1]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        int index = 0;
        Map<String, Integer> cnt = new HashMap<>();
        for (Question question : questions) {
            for (String comment : question.getComments()) {
                //Document commentDoc = new Document(comment);
                //for (Sentence sent : commentDoc.sentences()) {
                    //comment = sent.text();
                    if (comment.toLowerCase().contains("what kind") ||
                            comment.toLowerCase().contains("what type")) {
                        Document commentDoc = new Document(comment);
                        String[] commentNps = NlpUtils.extractNp(commentDoc, false);
                        String[] questionNps = question.getQuestionNounPhrases();

                        Set<String> commentNpsSet = new HashSet<>(
                                Arrays.asList(commentNps));
                        Set<String> questionNpsSet = new HashSet<>(
                                Arrays.asList(questionNps));

                        // Remove NPs not seen in the question.
                        // TODO(denxx): A better strategy might be to match by
                        // headwords.
                        commentNpsSet.retainAll(questionNpsSet);
                        questionNpsSet.removeAll(commentNpsSet);

                        if (!commentNpsSet.isEmpty() && !questionNpsSet.isEmpty()) {
                            ++index;
                            Set<String> allNps = new HashSet<>(questionNpsSet);
                            allNps.addAll(commentNpsSet);
                            for (String currentNp : allNps) {
                                int label = commentNpsSet.contains(currentNp) ? 1 : 0;
                                double e = entropy.getOrDefault(currentNp, 0.0);
                                long questionCount = Arrays.stream(questionNps)
                                        .filter(np -> np.equals(currentNp)).count();
                                int firstLocation = Arrays.asList(questionNps)
                                        .indexOf(currentNp);
                                double random = Math.random();
                                double prior = Math.log(freq.getOrDefault(currentNp, 1));
                                int words = currentNp.split("\\s").length;
                                System.out.println(label + " qid:" + index +
                                        " 1:" + e + " 2:" + questionCount +
                                        " 3:" + firstLocation + " 4:" + words + " 5:" + random + " 6:" + prior + " # '" + currentNp + "'");
                            }
                        }
                    } else {
//                    for (String np : NlpUtils.extractNp(new Document(comment), false)) {
//                        cnt.put(np, cnt.getOrDefault(np, 0) + 1);
//                    }
                    }
                //}
            }
        }

        for (Map.Entry<String, Integer> e : cnt.entrySet()) {
            System.err.println(e.getKey() + "\t" + e.getValue());
        }
    }
}
