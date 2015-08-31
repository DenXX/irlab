package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.AppConfig;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerScoring;
import edu.emory.mathcs.ir.qa.answerer.ranking.MaxentModelAnswerScorer;
import edu.emory.mathcs.ir.scraping.YahooAnswersScraper;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 8/20/15.
 */
public class TestTrainedAnswerSelectionModelApp {

    public static void main(String[] args) throws IOException {
        final String qaDataFile = args[0];
        final String retrivedQaFile = args[1];
        final String modelPath = args[2];
        final String indexPath = args[3];

        final PrintWriter out = new PrintWriter(
                new BufferedOutputStream(new FileOutputStream(args[4])));

        final AnswerScoring answerScorer = getAnswerScorer(modelPath, indexPath);

        Map<String, YahooAnswersScraper.QuestionAnswer> qaById =
                readQaData(qaDataFile);
        Map<String, List<String>> retrieved = readRetrivalResults(
                retrivedQaFile);

        for (Map.Entry<String, List<String>> retrievedList :
                retrieved.entrySet()) {
            final String qid = retrievedList.getKey();
            if (!qaById.containsKey(qid)) continue;

            final YahooAnswersScraper.QuestionAnswer queryQa = qaById.get(qid);
            final Question question = queryQa.getQuestion();
            List<AnswerScore> answers = new ArrayList<>();
            answers.add(new AnswerScore(queryQa.getAnswer(), 0, queryQa.qid));

            answers.addAll(retrievedList.getValue()
                    .stream()
                    .filter(x -> !x.equals(qid))
                    .filter(qaById::containsKey)
                    .map(retrievedQid -> new AnswerScore(
                            qaById.get(retrievedQid).getAnswer(), 0,
                            retrievedQid))
                    .collect(Collectors.toList()));

            AnswerScore[] rankedList = answers.stream()
                    .map(answer -> {
                        answer.score =
                                answerScorer.scoreAnswer(
                                        question, answer.answer);
                        return answer;
                    })
                    .sorted(Collections.reverseOrder())
                    .toArray(AnswerScore[]::new);

            if (rankedList.length > 1) {
                for (int rank = 1; rank <= rankedList.length; ++rank) {
                    out.println(
                            String.format("%s\tQ0\t%s\t%d\t%.5f\tEmoryIrLab",
                                    question.getId(), rankedList[rank - 1].id,
                                    rank, rankedList[rank - 1].score));
                }
            }
        }
        out.close();
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
            res.get(fields[0]).add(fields[2]);
        }
        input.close();
        return res;
    }

    private static AnswerScoring getAnswerScorer(
            String modelPath, String indexPath) throws IOException {
        //return new BM25AnswerScorer(reader);
        //return new RemoteAnswerScorer("octiron", 8080);

        return new MaxentModelAnswerScorer(modelPath,
                AppConfig.getFeatureGenerator());
    }

    private static class AnswerScore implements Comparable<AnswerScore> {
        public Answer answer;
        public double score;
        public String id;

        public AnswerScore(Answer answer, double score, String id) {
            this.answer = answer;
            this.score = score;
            this.id = id;
        }


        @Override
        public int compareTo(AnswerScore o) {
            return Double.compare(this.score, o.score);
        }
    }
}
