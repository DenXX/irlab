package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.annotators.EntityResolutionAnnotator;
import edu.emory.mathcs.clir.relextract.data.DeserializerInputProvider;
import edu.emory.mathcs.clir.relextract.data.QuestionAnswerAnnotation;
import edu.emory.mathcs.clir.relextract.processor.*;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import org.apache.commons.cli.*;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 9/12/14.
 */
public class RelationExtractionApp {

    /**
     * The name of the input filename command line argument.
     */
    public static final String INPUT_ARG = "input";

    /**
     * The name of the output filename command line argument.
     */
    public static final String OUTPUT_ARG = "output";

    public static final String PROCESSORS_ARG = "processors";

    // TODO(denxx): There should be a much better way to add options.
    private static Options getOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName("input_path")
                .withDescription("input file").create(INPUT_ARG));
        options.addOption(OptionBuilder.isRequired(true).hasArg().
                withArgName("output_path").withDescription("output file")
                .create(OUTPUT_ARG));
        options.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName("processors").withDescription("Processors to run")
                .create(PROCESSORS_ARG));
        options.addOption(OptionBuilder.hasArg()
                .withArgName(EntityResolutionAnnotator.LEXICON_PROPERTY)
                .withDescription("entity names lexicon file").create(
                        EntityResolutionAnnotator.LEXICON_PROPERTY));
        options.addOption(OptionBuilder.hasArg()
                .withArgName(ConcurrentProcessingRunner.NUM_THREADS_PROPERTY)
                .withDescription("Number of threads to use").create(
                        ConcurrentProcessingRunner.NUM_THREADS_PROPERTY));
        options.addOption(OptionBuilder.hasArg()
                .withArgName(KnowledgeBase.KB_PROPERTY)
                .withDescription("Apache Jena KB model location").create(
                        KnowledgeBase.KB_PROPERTY));
        return options;
    }

    private static CommandLine parseCommandLine(
            Options options, String[] args) throws ParseException{
        CommandLineParser parser = new GnuParser();
        return parser.parse(options, args);
    }

    private static Properties getProperties(CommandLine cmdline) {
        Properties props = new Properties();
        props.setProperty(INPUT_ARG, cmdline.getOptionValue(INPUT_ARG));
        props.setProperty(OUTPUT_ARG, cmdline.getOptionValue(OUTPUT_ARG));
        for (Option option : cmdline.getOptions()) {
            props.put(option.getArgName(), option.getValue());
        }

        return props;
    }

    private static void readSerializedDocuments(String inputPath) {
        try {
            final ObjectInputStream input =
                    new ObjectInputStream(
                            new BufferedInputStream(
                                    new GZIPInputStream(
                                            new FileInputStream(inputPath))));
            QuestionAnswerAnnotation document = null;
            int count = 0;
            while ((document =
                    (QuestionAnswerAnnotation) input.readObject()) != null) {
                ++count;
            }
            System.out.println("Total number of documents read is " + count);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void run(Properties props) throws Exception {
        WorkflowProcessor workflow = new WorkflowProcessor(props);
//        props.setProperty("nthreads", "24");
//        props.setProperty("ner.maxtime", "-1");
        for (String processor : props.getProperty(PROCESSORS_ARG).split(",")) {
            switch (processor) {
                case "entity":
                    workflow.addProcessor(new EntityAnnotationProcessor(props));
                    break;
                case "filter":
                    workflow.addProcessor(new FilterNotresolvedEntitiesProcessor(props));
                    break;
                case "relations":
                    workflow.addProcessor(new EntityRelationsProcessor(props));
                    break;
                case "parse":
                    workflow.addProcessor(new ParsingProcessor(props));
                    break;
                case "serialize":
                    workflow.addProcessor(new SerializerProcessor(props));
                    break;
                case "batchserialize":
                    workflow.addProcessor(new BatchSerializerProcessor(props));
                    break;
                case "print":
                    workflow.addProcessor(new PrintTextProcessor(props));
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "Processor " + processor + " doesn't exist!");
            }
        }
        workflow.freeze();

        try {
//            YahooAnswersWebscopeXmlInputProvider inputProvider =
//                    new YahooAnswersWebscopeXmlInputProvider(props);

//            DeserializerBatchInputProvider inputProvider =
//                    new DeserializerBatchInputProvider(props);

            DeserializerInputProvider inputProvider =
                    new DeserializerInputProvider(props);

            ConcurrentProcessingRunner runner =
                    new ConcurrentProcessingRunner(workflow, props);
//            SequentialProcessingRunner runner =
//                    new SequentialProcessingRunner(workflow, props);
            runner.run(inputProvider);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find input file.");
        } catch (XMLStreamException e) {
            System.err.println("Error parsing input XML file.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = getOptions();
        CommandLine cmdline = null;

        try {
            cmdline = parseCommandLine(options, args);
        } catch (ParseException exc) {
            System.err.println("Error parsing command line: " +
                    exc.getMessage());
            System.exit(-1);
        }

        RelationExtractionApp.run(getProperties(cmdline));
    }
}
