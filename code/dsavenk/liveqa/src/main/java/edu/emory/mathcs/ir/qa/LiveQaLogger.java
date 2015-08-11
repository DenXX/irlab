package edu.emory.mathcs.ir.qa;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

/**
 * Created by dsavenk on 8/11/15.
 */
public class LiveQaLogger {
    public static final Logger LOGGER =
            Logger.getLogger(LiveQaLogger.class.getName());

    public static final String LOG_FILE = "emory-test-01.log";

    static {
        FileHandler fileHandler;
        try {
            fileHandler = new FileHandler(LOG_FILE);
            fileHandler.setFormatter(new SimpleFormatter());
            LOGGER.addHandler(fileHandler);
            LOGGER.setUseParentHandlers(false);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
