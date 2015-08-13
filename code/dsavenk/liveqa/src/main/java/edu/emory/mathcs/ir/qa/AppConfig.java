package edu.emory.mathcs.ir.qa;

import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

/**
 * Static class that manages configuration options (including command line
 * options) of the application.
 */
public class AppConfig {
    /**
     * Application level properties.
     */
    public static final Properties PROPERTIES = new Properties();

    /**
     * Command line help argument.
     */
    public static final String HELP_PARAMETER = "help";
    public static final String HELP_PARAMETER_DESCRIPTION = "Show usage help";
    /**
     * Port to start LiveQA server on.
     */
    public static final String PORT_PARAMETER = "PORT";
    public static final String PORT_PARAMETER_DESCRIPTION =
            "LiveQA server port";

    /**
     * Bing API key parameter name.
     */
    public static final String BING_API_KEY_PARAMETER = "BING_SEARCH_API_KEY";
    public static final String BING_API_KEY_PARAMETER_DESCRIPTION
            = "Bing API key";

    /**
     * Initializes system properties with properties read from the resources
     * and overriden values found in the command line.
     * @param args Command line arguments.
     */
    public static void Init(String[] args) {
        // Trying to read application config file and exit on failure.
        try {
            PROPERTIES.load(AppConfig.class.getResourceAsStream(
                    "/qa-config.properties"));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }

        // Trying to parse command line arguments and print usage and exit if
        // unsuccessful.
        Options cmdLineOptions = AppConfig.getCommandLineOptions();
        Optional<CommandLine> cmdLine = Optional.empty();
        try {
            cmdLine = Optional.of(parseCommandLine(cmdLineOptions, args));
        } catch (ParseException e) {
            new HelpFormatter().printHelp(
                    "LiveQaServer", cmdLineOptions);
        }

        if (!cmdLine.isPresent()) System.exit(-1);
        AppConfig.updateProperties(cmdLine.get());
    }

    /**
     * Updates default propeties with flags specified through the command line.
     * @param parsedCommandLine Parsed command line option to update the current
     *                          set of properties.
     * @return Updated properties dictionary.
     */
    private static void updateProperties(CommandLine parsedCommandLine) {
        for (Option option : parsedCommandLine.getOptions()) {
            PROPERTIES.put(option.getArgName(),
                    option.hasArg() ? option.getValue() : "");
        }
    }

    /**
     * Returns application command line options as Common CLI Options object.
     * @return QuestionAnsweringApp command line options.
     */
    private static Options getCommandLineOptions() {
        Options options = new Options();
        options.addOption(Option.builder(HELP_PARAMETER)
                .desc(HELP_PARAMETER_DESCRIPTION).build());
        options.addOption(Option.builder(PORT_PARAMETER)
                .desc(PORT_PARAMETER_DESCRIPTION).hasArg().build());
        options.addOption(Option.builder(BING_API_KEY_PARAMETER)
                .desc(BING_API_KEY_PARAMETER_DESCRIPTION).hasArg().build());
        return options;
    }

    private static CommandLine parseCommandLine(Options options, String[] args)
            throws ParseException {
        return new DefaultParser().parse(options, args);
    }
}