package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.annotators.EntityResolutionAnnotator;
import edu.emory.mathcs.clir.relextract.data.QuestionAnswerAnnotation;
import edu.emory.mathcs.clir.relextract.data.YahooAnswersWebscopeXmlInputProvider;
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

    // TODO(denxx): There should be a much better way to add options.
    private static Options getOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName("input_path")
                .withDescription("input file").create(INPUT_ARG));
        options.addOption(OptionBuilder.isRequired(true).hasArg().
                withArgName("output_path").withDescription("output file")
                .create(OUTPUT_ARG));
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

    private static void run(Properties props) throws IOException {
        WorkflowProcessor workflow = new WorkflowProcessor(props);
        workflow.addProcessor(new EntityAnnotationProcessor(props));
        workflow.addProcessor(new FilterNotresolvedEntitiesProcessor(props));
        // workflow.addProcessor(new EntityRelationsProcessor(props));
        workflow.addProcessor(new ParsingProcessor(props));
        workflow.addProcessor(new SerializerProcessor(props));
        workflow.freeze();

        try {
            YahooAnswersWebscopeXmlInputProvider inputProvider =
                    new YahooAnswersWebscopeXmlInputProvider(props);
            //ConcurrentProcessingRunner runner =
            //        new ConcurrentProcessingRunner(workflow, props);
            SequentialProcessingRunner runner =
                    new SequentialProcessingRunner(workflow, props);
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
