package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.scraping.YahooAnswersScraper;

import java.io.*;
import java.util.Optional;

/**
 * Created by dsavenk on 8/28/15.
 */
public class DownloadQnAByIdApp {

    public static void main(String[] args) throws IOException {
        final BufferedReader input = new BufferedReader(
                new InputStreamReader(new FileInputStream(args[0])));
        final PrintWriter out = new PrintWriter(
                new BufferedOutputStream(new FileOutputStream(args[1])));
        String line;
        while ((line = input.readLine()) != null) {
            Optional<YahooAnswersScraper.QuestionAnswer> qna =
                    YahooAnswersScraper.GetQuestionAnswerData(line);
            if (qna.isPresent()) {
                YahooAnswersScraper.QuestionAnswer qa = qna.get();
                out.println(qa.toString());
            } else {
                System.err.println("QNA with id=" + line + " not found!");
            }
        }
    }
}
