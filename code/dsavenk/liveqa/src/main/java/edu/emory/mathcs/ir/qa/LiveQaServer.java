package edu.emory.mathcs.ir.qa;

import org.trec.liveqa.TrecLiveQaDemoServer;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by dsavenk on 8/5/15.
 */
public class LiveQaServer extends TrecLiveQaDemoServer {
    public static final String PARTICIPANT_ID = "emory-test-01";
    private static final String HOST = "0.0.0.0";
    private static final String LOG_FILE = "emory-test-01.log";
    private static final Logger logger =
            Logger.getLogger(LiveQaServer.class.getName());

    public LiveQaServer(String hostname, int port) throws IOException {
        super(hostname, port);
        InitLogger();
    }
    public LiveQaServer(int port) throws IOException {
        super(port);
        InitLogger();
    }

    private void InitLogger() throws IOException {
        FileHandler fileHandler = new FileHandler(LOG_FILE);
        fileHandler.setFormatter(new SimpleFormatter());
        logger.addHandler(fileHandler);
        logger.setUseParentHandlers(false);
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
        logger.info(String.join("\t",
                new String[] {qid, category, title, body}));
        return new TrecLiveQaDemoServer.AnswerAndResources(
                "I don't really know", "None");
    }

    private String normalize(String text) {
        return text.replace("\n", " ").replace("\t", " ");
    }

    public static void main(String args[]) throws IOException {
        LiveQaServer server =
                new LiveQaServer(HOST, args.length == 0 ?
                        DEFAULT_PORT :
                        Integer.parseInt(args[0]));
        server.start();
        System.in.read();
        server.stop();
    }
}
