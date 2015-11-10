package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.AppConfig;
import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.QuestionAnswering;
import edu.emory.mathcs.ir.qa.answerer.YahooAnswersAndWebQuestionsAnswerer;

import java.io.*;

/**
 * Created by dsavenk on 11/9/15.
 */
public class ConsoleQaApp {

    public static void main(String[] args) throws IOException {
        QuestionAnswering qa =
                new YahooAnswersAndWebQuestionsAnswerer(
                        AppConfig.getAnswerSelector());
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                        new FileInputStream(args[0])));

        String line = reader.readLine();  // Skipping the header row.
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            if (fields.length < 5 || fields[0].isEmpty()) continue;

            String qid = fields[0];
            String title = fields[1];
            String body = fields[2];
            String best_answer = fields[3];
            String category = fields[4];

            Answer answer = qa.GetAnswer(
                    new Question(qid, title, body, category));
            LiveQaLogger.LOGGER.fine(String.format("ANSWER\t%s", answer.getAnswer()));
        }
    }
}
