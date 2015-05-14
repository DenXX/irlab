package edu.emory.mathcs.clir.qa;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * Main question answering application.
 */
public class QuestionAnsweringApp {

    private static CommandLine parseCommandLine(Options options, String[] args) throws ParseException {
        return new DefaultParser().parse(options, args);

    }

    public static void main(String[] args) {
        // Trying to read application config file and exit on failure.
        Properties properties = new Properties();
        try {
            properties.load(QuestionAnsweringApp.class.getResourceAsStream("/qa-config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Trying to parse command line arguments and print usage and exit if unsuccessful.
        Options cmdLineOptions = QuestionAnsweringAppConfig.getCommandLineOptions();
        Optional<CommandLine> cmdLine = Optional.empty();
        try {
            cmdLine = Optional.of(parseCommandLine(cmdLineOptions, args));
        } catch (ParseException e) {
            new HelpFormatter().printHelp("QuestionAnsweringApp", cmdLineOptions);
        }

        if (!cmdLine.isPresent()) System.exit(-1);

        properties = QuestionAnsweringAppConfig.getProperties(properties, cmdLine.get());
    }
}
