package edu.emory.mathcs.clir.relextract;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDB;
import com.hp.hpl.jena.tdb.TDBFactory;
import com.hp.hpl.jena.tdb.base.file.Location;
import edu.emory.mathcs.clir.relextract.annotators.EntityResolutionAnnotator;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationSerializer;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.cli.*;
import org.mortbay.resource.Resource;

import java.io.*;
import java.util.Properties;

/**
 * Created by dsavenk on 9/12/14.
 */
public class RelationExtractionApp {

    private static final String INPUT_ARG = "input";
    private static final String OUTPUT_ARG = "output";

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.hasArg().withArgName("input_path")
                .withDescription("input file").create(INPUT_ARG));
        options.addOption(OptionBuilder.hasArg().withArgName("output_path")
                .withDescription("output file").create(OUTPUT_ARG));
        options.addOption(OptionBuilder.hasArg()
                .withArgName(EntityResolutionAnnotator.LEXICON_PROPERTY)
                .withDescription("entity names lexicon file").create(
                        EntityResolutionAnnotator.LEXICON_PROPERTY));
        return options;
    }

    private static CommandLine parseCommandLine(
            Options options, String[] args) throws ParseException{
        CommandLineParser parser = new GnuParser();
        return parser.parse(options, args);
    }

    private static Properties getProperties(CommandLine cmdline) {
        Properties props = new Properties();
        // Add the new annotator.
        props.setProperty("customAnnotatorClass.entityres",
                "edu.emory.mathcs.clir.relextract.annotators." +
                        "EntityResolutionAnnotator");
        props.setProperty("customAnnotatorClass.span",
                "edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator");

        for (Option option : cmdline.getOptions()) {
            props.put(option.getArgName(), option.getValue());
        }

        return props;
    }

    private static void processInput(String inputPath, String outputPath,
                                     Properties props) {

        {
            Dataset dataset = TDBFactory.createDataset(inputPath);
            dataset.begin(ReadWrite.READ);
            Model model = dataset.getDefaultModel();
            StmtIterator iter = model.listStatements(new SimpleSelector(
                    model.getResource("http://rdf.freebase.com/ns/m.0100vzmn"),
                    null, (RDFNode)null));
            while (iter.hasNext()) {
                Statement st = iter.nextStatement();
                System.out.println(st);
            }
            dataset.end();
            System.exit(-1);
        }

        props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, " +
                "truecase, ner, parse, dcoref, span, entityres");
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);
        try {
            BufferedReader input = new BufferedReader(
                    new FileReader(inputPath));
            ObjectOutputStream out = new ObjectOutputStream(
                    new FileOutputStream(outputPath));
            String curLine = null;
            while ((curLine = input.readLine()) != null) {
                Annotation document = new Annotation(curLine);
                pipeline.annotate(document);
                out.writeObject(document);
            }
        } catch (FileNotFoundException exc) {
            System.err.println("File not found: " + inputPath);
            System.exit(-1);
        } catch (IOException exc) {
            System.err.println("Error reading the file: " + inputPath);
            System.exit(-1);
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

        if (!cmdline.hasOption(INPUT_ARG)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(RelationExtractionApp.class.getCanonicalName(),
                    options);
            System.exit(-1);
        }

        processInput(cmdline.getOptionValue(INPUT_ARG),
                cmdline.getOptionValue(OUTPUT_ARG), getProperties(cmdline));
    }
}
