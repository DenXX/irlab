package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.scraping.YahooAnswersScraper;

import java.io.*;
import java.util.Optional;

/**
 * Created by dsavenk on 8/28/15.
 */
public class DownloadQnAByIdApp {

    public static void main(String[] args) throws IOException {
        System.err.println("Downloading QnAs...");
        final BufferedReader input = new BufferedReader(
                new InputStreamReader(new FileInputStream(args[0])));
        final PrintWriter out = new PrintWriter(
                new BufferedOutputStream(new FileOutputStream(args[1])));
        String line;
        int index = 0;
        while ((line = input.readLine()) != null) {
            Optional<YahooAnswersScraper.QuestionAnswer> qna =
                    YahooAnswersScraper.GetQuestionAnswerData(line);
            if (qna.isPresent()) {
                YahooAnswersScraper.QuestionAnswer qa = qna.get();
                out.println(qa.toString());
            } else {
                System.err.println("QNA with id=" + line + " not found!");
            }
            if (++index % 50 == 0) {
                System.err.println(String.format("%d QIDs downloaded", index));
            }
        }
        out.close();
    }
}
