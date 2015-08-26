package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.stream.Collectors;

/**
 * Connects to the remote computer to get scores for the given question-answer
 * pair.
 */
public class RemoteAnswerScorer implements AnswerScoring {
    private final String host_;
    private final int port_;

    /**
     * Creates remote answer scorer, that will connect to the given port of the
     * given host and send QnA pair for scoring.
     *
     * @param host The hostname to connect to.
     * @param port The port to connect to on the remote host.
     * @throws IOException
     */
    public RemoteAnswerScorer(String host, int port) throws IOException {
        host_ = host;
        port_ = port;
    }

    @Override
    public double scoreAnswer(Question question, Answer answer) {
        Socket socket = null;
        try {
            socket = new Socket(host_, port_);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));
            PrintWriter out =
                    new PrintWriter(socket.getOutputStream(), true);
            out.println(
                    question.getTitle().concat(
                            question.getBody()).getLemmaList(false).stream()
                            .collect(Collectors.joining(" "))
                            + "\t" +
                            answer.getAnswer().getLemmaList(false).stream()
                                    .collect(Collectors.joining(" ")));
            String scoreStr = in.readLine();
            socket.close();
            return Double.parseDouble(scoreStr);
        } catch (IOException e) {
            LiveQaLogger.LOGGER.warning(e.getMessage());
        }
        return 0.0;
    }
}
