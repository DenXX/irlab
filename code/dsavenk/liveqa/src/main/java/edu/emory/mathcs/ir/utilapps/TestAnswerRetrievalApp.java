package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.QuestionAnswering;
import edu.emory.mathcs.ir.qa.answerer.yahooanswers.YahooAnswersBasedQuestionAnswerer;
import edu.emory.mathcs.ir.scraping.YahooAnswersScraper;
import edu.stanford.nlp.util.Sets;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.*;

/**
 * Created by dsavenk on 8/20/15.
 */
public class TestAnswerRetrievalApp {
    public static void main(String[] args) throws IOException {
        final String qaDataFile = args[0];
        final String retrivedQaFile = args[1];
        final String modelPath = args[2];
        final String indexPath = args[3];

        final QuestionAnswering questionAnswerer =
                getQuestionAnswerer(indexPath, modelPath);

        Map<String, YahooAnswersScraper.QuestionAnswer> qaById =
                readQaData(qaDataFile);
        Set<String> retrieved = readRetrivalResults(
                retrivedQaFile).keySet();

        double score = 0;
        int count = 0;
        for (String qid : retrieved) {
            if (!qaById.containsKey(qid)) continue;

            final YahooAnswersScraper.QuestionAnswer queryQa = qaById.get(qid);
            final Question question = queryQa.getQuestion();
            final Answer correctAnswer = queryQa.getAnswer();
            Answer answer = questionAnswerer.GetAnswer(question);
            score += getAnswerScore(correctAnswer, answer);
            ++count;
        }
        System.out.println("Average score = " + (score / count));
    }

    private static double getAnswerScore(Answer correctAnswer, Answer answer) {
        Set<String> correctLemmaSet =
                correctAnswer.getAnswer().getLemmaSet(true);
        if (correctLemmaSet.size() > 0) {
            Set<String> predictedLemmaSet =
                    answer.getAnswer().getLemmaSet(true);
            return 1.0 * Sets.intersection(
                    correctLemmaSet, predictedLemmaSet).size() /
                    correctLemmaSet.size();
        }
        return 1.0;
    }

    private static Map<String, YahooAnswersScraper.QuestionAnswer> readQaData(
            String qaDataFile) throws IOException {
        Map<String, YahooAnswersScraper.QuestionAnswer> res = new HashMap<>();
        BufferedReader input = new BufferedReader(
                new InputStreamReader(new FileInputStream(qaDataFile)));
        String line;
        while ((line = input.readLine()) != null) {
            YahooAnswersScraper.QuestionAnswer qa =
                    YahooAnswersScraper.QuestionAnswer.parseFromString(line);
            res.put(qa.qid, qa);
        }
        input.close();
        return res;
    }

    private static Map<String, List<String>> readRetrivalResults(
            String retrievedQidFile) throws IOException {
        Map<String, List<String>> res = new HashMap<>();
        BufferedReader input = new BufferedReader(
                new InputStreamReader(new FileInputStream(retrievedQidFile)));
        String line;
        while ((line = input.readLine()) != null) {
            String[] fields = line.split("\t");
            if (!res.containsKey(fields[0])) {
                res.put(fields[0], new ArrayList<>());
            }
            res.get(fields[0]).add(fields[3]);
        }
        input.close();
        return res;
    }

    private static QuestionAnswering getQuestionAnswerer(
            String indexPath, String modelPath)
            throws IOException {
        IndexReader reader = DirectoryReader.open(
                FSDirectory.open(FileSystems.getDefault().getPath(indexPath)));
        return new YahooAnswersBasedQuestionAnswerer(reader, modelPath);
    }
}
