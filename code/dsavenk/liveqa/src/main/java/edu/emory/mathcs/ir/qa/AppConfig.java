package edu.emory.mathcs.ir.qa;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;

import java.util.Properties;

/**
 * Static class that manages configuration options (including command line
 * options) of the QuestionAnsweringApp.
 */
public class AppConfig {
    public static final String HELP_PARAMETER = "help";
    public static final String HELP_PARAMETER_DESCRIPTION = "Show usage help";

    /**
     * Updates default propeties with flags specified through the command line.
     * @param defaultPropeties Current set of properties. If command line has an
     *                         option with the same name as one of the existing
     *                         propeties, it will be overridden.
     * @param parsedCommandLine Parsed command line option to update the current
     *                          set of properties.
     * @return Updated properties dictionary.
     */
    public static Properties getProperties(Properties defaultPropeties,
                                           CommandLine parsedCommandLine) {
        for (Option option : parsedCommandLine.getOptions()) {
            defaultPropeties.put(option.getArgName(),
                    option.hasArg() ? option.getValue() : "");
        }
        return defaultPropeties;
    }

    /**
     * Returns application command line options as Common CLI Options object.
     * @return QuestionAnsweringApp command line options.
     */
    public static Options getCommandLineOptions() {
        Options options = new Options();
        options.addOption(Option.builder(HELP_PARAMETER)
                        .desc(HELP_PARAMETER_DESCRIPTION).build());
        return options;
    }
}