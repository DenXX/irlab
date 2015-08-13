package edu.emory.mathcs.ir.qa;

import edu.emory.mathcs.ir.qa.answerer.QuestionAnswering;
import edu.emory.mathcs.ir.qa.answerer.yahooanswers.YahooAnswersBasedQuestionAnswerer;
import org.trec.liveqa.TrecLiveQaDemoServer;

import java.io.IOException;

/**
 * Created by dsavenk on 8/5/15.
 */
public class LiveQaServer extends TrecLiveQaDemoServer {
    public static final String PARTICIPANT_ID = "emory-test-01";
    private static final String HOST = "0.0.0.0";

    // QA system.
    private final QuestionAnswering qa_ =
            new YahooAnswersBasedQuestionAnswerer();

    public LiveQaServer(String hostname, int port) throws IOException {
        super(hostname, port);
    }
    public LiveQaServer(int port) throws IOException {
        super(port);
    }

    @Override
    protected String participantId() {
        return LiveQaServer.PARTICIPANT_ID;
    }

    @Override
    protected AnswerAndResources getAnswerAndResources(
            String qid, String title, String body, String category)
            throws InterruptedException {
        if (qid == null) return null;

        qid = normalize(qid);
        title = normalize(title);
        body = normalize(body);
        category = normalize(category);
        final Question question = new Question(qid, title, body, category);
        LiveQaLogger.LOGGER.info(question.toString());
        final Answer answer = qa_.GetAnswer(question);
        LiveQaLogger.LOGGER.info(answer.toString());

        return new TrecLiveQaDemoServer.AnswerAndResources(
                answer.getAnswer().text, answer.getSource());
    }

    private String normalize(String text) {
        return text.replace("\n", " ").replace("\t", " ");
    }

    public static void main(String args[]) throws IOException {
        AppConfig.Init(args);
        final int port = Integer.parseInt(
                AppConfig.PROPERTIES.getProperty(AppConfig.PORT_PARAMETER));
        LiveQaServer server = new LiveQaServer(HOST, port);
        server.start();
        System.in.read();
        server.stop();
    }
}
