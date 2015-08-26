package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.answerer.query.TitleNoStopwordsQueryFormulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 8/26/15.
 */
public class Question2QueryTestApp {
    public static void main(String[] args) throws IOException {
        final BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in));
        QueryFormulation queryFormulation =
                new TitleNoStopwordsQueryFormulator();
        String line;
        while ((line = input.readLine()) != null) {
            final String[] lineFields = line.split("\t");
            final Question question =
                    new Question("", lineFields[0], lineFields[1], "");
            final Answer answer =
                    new Answer(lineFields[2], "");

            Socket socket = new Socket("octiron", 8080);
            PrintWriter out =
                    new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            out.println(question.getTitle().concat(question.getBody())
                    .getLemmaList(false)
                    .stream().collect(Collectors.joining(" ")));
            out.println(answer.getAnswer()
                    .getLemmaList(false)
                    .stream().collect(Collectors.joining(" ")));
            String scoreStr = in.readLine();
            double score = Double.parseDouble(scoreStr);
            socket.close();
            System.err.println(score);
        }
    }
}
