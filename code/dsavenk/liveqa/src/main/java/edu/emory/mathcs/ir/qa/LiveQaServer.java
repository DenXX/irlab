package edu.emory.mathcs.ir.qa;

import edu.emory.mathcs.ir.qa.answerer.QuestionAnswering;
import edu.emory.mathcs.ir.qa.answerer.yahooanswers.YahooAnswersBasedQuestionAnswerer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.trec.liveqa.TrecLiveQaDemoServer;

import java.io.IOException;
import java.nio.file.FileSystems;

/**
 * Created by dsavenk on 8/5/15.
 */
public class LiveQaServer extends TrecLiveQaDemoServer {
    public static final String PARTICIPANT_ID = "emory-test-01";
    private static final String HOST = "0.0.0.0";

    // QA system.
    private QuestionAnswering qa_;

    public LiveQaServer(String hostname, int port) throws IOException {
        super(hostname, port);
        final String modelPath = AppConfig.PROPERTIES.getProperty(
                AppConfig.RANKING_MODEL_PATH_PARAMETER);
        final Directory directory = FSDirectory.open(
                FileSystems.getDefault().getPath(
                        AppConfig.QA_INDEX_DIRECTORY_PARAMETER));
        final IndexReader indexReader = DirectoryReader.open(directory);
        qa_ = new YahooAnswersBasedQuestionAnswerer(indexReader, modelPath);
    }
    public LiveQaServer(int port) throws IOException {
        super(port);
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
}
