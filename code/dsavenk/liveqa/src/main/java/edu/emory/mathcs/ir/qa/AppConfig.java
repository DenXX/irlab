package edu.emory.mathcs.ir.qa;

import edu.emory.mathcs.ir.qa.answerer.query.AddCategoryNameToQuery;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.answerer.query.SimpleQueryFormulator;
import edu.emory.mathcs.ir.qa.answerer.query.TopIdfTermsQueryFormulator;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerSelection;
import edu.emory.mathcs.ir.qa.answerer.ranking.FeatureBasedAnswerSelector;
import edu.emory.mathcs.ir.qa.answerer.ranking.RemoteAnswerScorer;
import edu.emory.mathcs.ir.qa.ml.*;
import org.apache.commons.cli.*;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;
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

    public static final String LIVEQA_CATEGORIES_PARAMETER =
            "LIVEQA_CATEGORIES";
    public static final String LIVEQA_CATEGORIES_PARAMETER_DESCRIPTION =
            "The list of Yahoo! Answers top-level categories selected for " +
                    "the LiveQA TREC";

    public static final String ANNOTATORS_PARAMETER = "ANNOTATORS";
    public static final String ANNOTATORS_PARAMETER_DESCRIPTION =
            "The list of Stanford CoreNLP annotators to apply to text";

    public static final String RANKING_MODEL_PATH_PARAMETER =
            "ANSWER_RANKING_MODEL";
    public static final String RANKING_MODEL_PATH_PARAMETER_DESCRIPTION =
            "Trained model for answer ranking";

    public static final String QA_INDEX_DIRECTORY_PARAMETER =
            "QA_INDEX_DIRECTORY";
    public static final String QA_INDEX_DIRECTORY_PARAMETER_DESCRIPTION =
            "Location of QnA index to use for various statistics";

    public static final String LSTM_MODEL_SERVER_PARAMETER =
            "LSTM_MODEL_SERVER";
    public static final String LSTM_MODEL_SERVER_PARAMETER_DESCRIPTION =
            "The name of the server where LSTM model is launched";

    public static final String LSTM_MODEL_PORT_PARAMETER =
            "LSTM_MODEL_PORT";
    public static final String LSTM_MODEL_PORT_PARAMETER_DESCRIPTION =
            "The port number on the server where LSTM model is launched";

    public static final String KB_MODEL_PARAMETER = "KB_MODEL";
    public static final String KB_MODEL_PARAMETER_DESCRIPTION =
            "Location of Freebase Jena model";

    public static final String KB_INDEX_PARAMETER = "KB_INDEX";
    public static final String KB_INDEX_PARAMETER_DESCRIPTION =
            "Location of Freebase names index";

    public static final String SIMILAR_QUESTIONS_COUNT_PARAMETER =
            "SIMILAR_QUESTIONS_COUNT";
    public static final String SIMILAR_QUESTIONS_COUNT_PARAMETER_DESCRIPTION =
            "The number of similar questions to retrieve using Yahoo!Answers " +
                    "search";

    public static final String WEB_SEARCH_TOPN_PARAMETER =
            "WEB_SEARCH_TOPN";
    public static final String WEB_SEARCH_TOPN_PARAMETER_DESCRIPTION =
            "The number of web search results to retrieve";

    public static final String NPMI_DICTIONARY_LOCATION_PARAMETER =
            "NPMI_DICTIONARY_LOCATION";
    public static final String NPMI_DICTIONARY_LOCATION_PARAMETER_DESCRIPTION =
            "Location of <question term>\t<answer term>\t<npmi> dictionary";


    private static IndexReader qaIndexReader_;

    static {
        Init();
    }

    private static void Init() {
        // Trying to read application config file and exit on failure.
        try {
            PROPERTIES.load(AppConfig.class.getResourceAsStream(
                    "/qa-config.properties"));
            qaIndexReader_ = DirectoryReader.open(
                    FSDirectory.open(
                            FileSystems.getDefault().getPath(
                                    PROPERTIES.getProperty(
                                            QA_INDEX_DIRECTORY_PARAMETER))));
        } catch (IOException e) {
            e.printStackTrace();
            System.exit(-1);
        }
    }

    /**
     * Initializes system properties with properties read from the resources
     * and overriden values found in the command line.
     * @param args Command line arguments.
     */
    public static void Init(String[] args) {
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
        options.addOption(Option.builder(LIVEQA_CATEGORIES_PARAMETER)
                .desc(LIVEQA_CATEGORIES_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(ANNOTATORS_PARAMETER)
                .desc(ANNOTATORS_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(RANKING_MODEL_PATH_PARAMETER)
                .desc(RANKING_MODEL_PATH_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(QA_INDEX_DIRECTORY_PARAMETER)
                .desc(QA_INDEX_DIRECTORY_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(LSTM_MODEL_SERVER_PARAMETER)
                .desc(LSTM_MODEL_SERVER_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(LSTM_MODEL_PORT_PARAMETER)
                .desc(LSTM_MODEL_PORT_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(KB_MODEL_PARAMETER)
                .desc(KB_MODEL_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(KB_MODEL_PARAMETER)
                .desc(KB_MODEL_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(SIMILAR_QUESTIONS_COUNT_PARAMETER)
                .desc(SIMILAR_QUESTIONS_COUNT_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(WEB_SEARCH_TOPN_PARAMETER)
                .desc(WEB_SEARCH_TOPN_PARAMETER_DESCRIPTION).hasArg()
                .build());
        options.addOption(Option.builder(NPMI_DICTIONARY_LOCATION_PARAMETER)
                .desc(NPMI_DICTIONARY_LOCATION_PARAMETER_DESCRIPTION).hasArg()
                .build());
        return options;
    }

    private static CommandLine parseCommandLine(Options options, String[] args)
            throws ParseException {
        return new DefaultParser().parse(options, args);
    }

    public static FeatureGeneration getFeatureGenerator() throws IOException {
        int lstmPort = Integer.parseInt(AppConfig.PROPERTIES.getProperty(
                AppConfig.LSTM_MODEL_PORT_PARAMETER));
        return new CombinerFeatureGenerator(
                new LemmaPairsFeatureGenerator(),
                new BM25FeatureGenerator(getQuestionAnswerIndexReader()),
                new MatchesFeatureGenerator(),
                new NpmiDictionaryMatchesFeatureGenerator(
                        AppConfig.PROPERTIES.getProperty(
                                NPMI_DICTIONARY_LOCATION_PARAMETER)),
                new CategoryMatchFeatureGenerator(),
                new PageTitleMatchFeatureGenerator(),
                //new NamedEntityTypesFeatureGenerator(),
                // new ReverbTriplesFeatureGenerator(reverbIndexLocation),
                new AnswerStatsFeatureGenerator(),
                new AnswerScorerBasedFeatureGenerator("lstm_score=",
                        new RemoteAnswerScorer(
                                AppConfig.PROPERTIES.getProperty(
                                        AppConfig.LSTM_MODEL_SERVER_PARAMETER),
                                lstmPort))
        );
    }

    private static IndexReader getQuestionAnswerIndexReader() {
        return qaIndexReader_;
    }

    public static AnswerSelection getAnswerSelector() throws IOException {
        return new FeatureBasedAnswerSelector(
                PROPERTIES.getProperty(RANKING_MODEL_PATH_PARAMETER),
                getFeatureGenerator());
    }

    public static QueryFormulation[] getYaQueryFormulators() {
        return new QueryFormulation[] {
                new SimpleQueryFormulator(true, false),
                new SimpleQueryFormulator(false, false),
                new SimpleQueryFormulator(true, true),
                new SimpleQueryFormulator(false, true),
                new AddCategoryNameToQuery(new SimpleQueryFormulator(false, true)),
                new AddCategoryNameToQuery(new SimpleQueryFormulator(true, true)),
                new TopIdfTermsQueryFormulator(
                        getQuestionAnswerIndexReader(), true, 5),
                new TopIdfTermsQueryFormulator(
                        getQuestionAnswerIndexReader(), false, 5),
        };
    }

    public static QueryFormulation[] getWebQueryFormulators() {
        return new QueryFormulation[]{
                new SimpleQueryFormulator(true, false),
                new SimpleQueryFormulator(false, false),
        };
    }
}