package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.data.DeserializerInputProvider;
import edu.emory.mathcs.clir.relextract.processor.*;
import org.apache.commons.cli.*;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.Properties;

/**
 * Created by dsavenk on 9/12/14.
 */
public class RelationExtractionApp {

    // TODO(denxx): There should be a much better way to add options.
    private static Options getOptions() {
        return AppParameters.options;
    }

    private static CommandLine parseCommandLine(
            Options options, String[] args) throws ParseException{
        CommandLineParser parser = new GnuParser();
        return parser.parse(options, args);
    }

    private static Properties getProperties(CommandLine cmdline) {
        Properties props = new Properties();
        for (Option option : cmdline.getOptions()) {
            props.put(option.getArgName(), option.getValue());
        }

        return props;
    }

    private static void run(Properties props) throws Exception {
        WorkflowProcessor workflow = new WorkflowProcessor(props);
//        props.setProperty("nthreads", "24");
//        props.setProperty("ner.maxtime", "-1");
        for (String processor :props.getProperty(
                AppParameters.PROCESSORS_PARAMETER).split(",")) {
            switch (processor) {
                case "entity":
                    workflow.addProcessor(new EntityAnnotationProcessor(props));
                    break;
                case "filter":
                    workflow.addProcessor(new FilterNotresolvedEntitiesProcessor(props));
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
