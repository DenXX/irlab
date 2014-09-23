package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.annotators.EntityResolutionAnnotator;
import edu.emory.mathcs.clir.relextract.data.QuestionAnswerAnnotation;
import edu.emory.mathcs.clir.relextract.data.YahooWebscopeYAnswersDataset;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import org.apache.commons.cli.*;

import javax.xml.stream.XMLStreamException;
import java.io.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

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

    private static void readSerializedDocuments(String inputPath) {
        try {
            ObjectInputStream input =
                    new ObjectInputStream(
                            new BufferedInputStream(
                                    new GZIPInputStream(
                                            new FileInputStream(inputPath))));
            QuestionAnswerAnnotation doc = null;
            while ((doc = (QuestionAnswerAnnotation)input.readObject())
                    != null) {
                System.out.println(doc.toString());
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static void processInput(String inputPath, String outputPath,
                                     Properties props) {
        props.put("annotators", "tokenize, cleanxml, ssplit, pos, lemma, " +
                "ner, parse, dcoref, span, entityres");
        props.setProperty("clean.allowflawedxml", "true");
        final StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // Use all available physical processes.
        final int numThreads =
                Runtime.getRuntime().availableProcessors();
        final ExecutorService e = Executors.newFixedThreadPool(numThreads);

        try {
            YahooWebscopeYAnswersDataset dataset =
                    new YahooWebscopeYAnswersDataset(
                            new BufferedInputStream(
                                    new FileInputStream(inputPath)));
            // Create output stream for annotated documents serialization.
            final ObjectOutputStream out =
                    new ObjectOutputStream(
                            new BufferedOutputStream(
                                    new GZIPOutputStream(
                                            new FileOutputStream(outputPath))));

            // Counter for number of Q&A pairs processed.
            final AtomicInteger count = new AtomicInteger(0);

            final long startTime = System.currentTimeMillis();
            for (final QuestionAnswerAnnotation document : dataset) {
                if (document == null) continue;
                e.execute(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            pipeline.annotate(document);
                            synchronized (out) {
                                out.writeObject(document);
                            }
                            int curCnt = count.incrementAndGet();
                            if (curCnt % 1000 == 0) {
                                final long curTime = System.currentTimeMillis();
                                System.out.println("Processed " + curCnt +
                                        " at " + (1000.0 * curCnt) /
                                        (curTime - startTime) + " docs/sec");
                            }
                        } catch (Exception exc) {
                            // Sometimes annotators fail with an exception,
                            // let's skip such Q&A pairs. Example is
                            // cleanxml annotator, which for some reason
                            // checks correctness of XML tags and throws an
                            // exception if something goes wrong.
                            System.err.println(exc);
                        }
                    }
                });
            }
            e.shutdown();
            // Wait for all threads to finish.
            e.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            // Write null so we can read it and determine the end of the input.
            out.writeObject(null);
            out.close();
        } catch (FileNotFoundException exc) {
            e.shutdown();
            System.err.println("File not found: " + inputPath);
            System.exit(-1);
        } catch (IOException exc) {
            e.shutdown();
            System.err.println("Error reading the file: " + inputPath);
            System.exit(-1);
        } catch (XMLStreamException exc) {
            e.shutdown();
            System.err.println("Error parsing XML file: " + inputPath);
            System.exit(-1);
        } catch (InterruptedException exc) {
            exc.printStackTrace();
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

        //readSerializedDocuments(cmdline.getOptionValue(INPUT_ARG));

        processInput(cmdline.getOptionValue(INPUT_ARG),
                cmdline.getOptionValue(OUTPUT_ARG), getProperties(cmdline));
    }
}
