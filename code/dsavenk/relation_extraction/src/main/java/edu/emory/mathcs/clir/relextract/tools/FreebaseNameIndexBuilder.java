package edu.emory.mathcs.clir.relextract.tools;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.graph.Triple;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.jena.riot.Lang;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.jena.riot.lang.PipedRDFIterator;
import org.apache.jena.riot.lang.PipedRDFStream;
import org.apache.jena.riot.lang.PipedTriplesStream;

import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Reads Freebase RDF data dump and extracts all entity names, then for each
 * name it outputs all the corresponding entities with a number - total number
 * of triples for the given entity.
 */
public class FreebaseNameIndexBuilder {

    // Names of input and output file attributes.
    private static final String INPUT_ARG = "freebase";
    private static final String OUTPUT_ARG = "output";

    private static Options getOptions() {
        Options options = new Options();
        options.addOption(OptionBuilder.hasArg().withArgName("freebase_path")
                .withDescription("freebase RDF file").create(INPUT_ARG));
        options.addOption(OptionBuilder.hasArg().withArgName("ouput_file")
                .withDescription("output file").create(OUTPUT_ARG));
        return options;
    }

    private static CommandLine parseCommandLine(
            Options options, String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        return parser.parse(options, args);
    }

    private static void processFreebaseDump(final String freebase_dump_file,
                                            String outputFile) {
        PipedRDFIterator<Triple> iter = new PipedRDFIterator<Triple>();
        final PipedRDFStream<Triple> triplesStream =
                new PipedTriplesStream(iter);

        // PipedRDFStream and PipedRDFIterator need to be on different threads
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Create a runnable for our parser thread
        Runnable parser = new Runnable() {
            @Override
            public void run() {
                // Call the parsing process.
                try {
                    RDFDataMgr.parse(triplesStream,
                            new CompressorStreamFactory()
                                    .createCompressorInputStream(
                                            CompressorStreamFactory.GZIP,
                                            new BufferedInputStream(
                                                new FileInputStream(
                                                    freebase_dump_file))),
                            "http://rdf.freebase.com/ns/",
                            Lang.TURTLE);
                } catch (CompressorException e) {
                    System.err.println("Cannot decompress file: " +
                            e.getMessage());
                    return;
                } catch (FileNotFoundException e) {
                    System.err.println("File not found: " + freebase_dump_file);
                    return;
                }
            }
        };

        // Start the parser on another thread
        executor.submit(parser);

        HashMap<String, Long> entityTriplesCount = new HashMap<String, Long>();
        HashMap<String, Set<String>> nameEntityIndex =
                new HashMap<String, Set<String>>();
        while (iter.hasNext()) {
            Triple triple = iter.next();
            String subject =
                    "/" + triple.getSubject().getLocalName().replace(".", "/");
            String predicate =
                    "/" + triple.getPredicate().getLocalName()
                            .replace(".", "/");
            String object = null;
            try {
                if (triple.getObject().isLiteral()) {
                    object = triple.getObject().getLiteralValue().toString();
                } else if (triple.getObject().isVariable()) {
                    object = "/" +
                            triple.getObject().getLocalName().replace(".", "/");
                } else {
                    object = triple.getObject().toString();
                }
                if (!entityTriplesCount.containsKey(subject)) {
                    entityTriplesCount.put(subject, 1L);
                } else {
                    entityTriplesCount.put(subject,
                            entityTriplesCount.get(subject) + 1L);
                }

                if (subject.startsWith("/m/") &&
                        (predicate.equals("/type/object/name") ||
                                predicate.equals("/common/topic/alias"))) {
                    if (!nameEntityIndex.containsKey(object)) {
                        nameEntityIndex.put(object, new HashSet<String>());
                    }
                    nameEntityIndex.get(object).add(subject);
                }
            } catch (DatatypeFormatException e) {
                System.err.println("Error: " + e.getMessage());
            }
        }

        PrintWriter out = null;
        try {
            out = new PrintWriter(
                    new CompressorStreamFactory().createCompressorOutputStream(
                            CompressorStreamFactory.GZIP,
                            new BufferedOutputStream(
                                    new FileOutputStream(outputFile, false))));
        } catch (CompressorException e) {
            System.err.println("Cannot create output compressor: " +
                    e.getMessage());
            System.exit(-1);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot create output file: " + outputFile);
            System.exit(-1);
        }

        for (String phrase : nameEntityIndex.keySet()) {
            out.print(phrase.replace("\t", " ") + "\t");
            for (String entity : nameEntityIndex.get(phrase)) {
                out.print(entity + '\t' + entityTriplesCount.get(entity) +
                        '\t');
            }
            out.println();
        }
        executor.shutdown();
        out.close();
    }

    public static void main(String[] args) {
        Options options = getOptions();
        CommandLine cmdline = null;
        try {
            cmdline = parseCommandLine(options, args);
        } catch (ParseException exc) {
            System.err.println("Error parsing command line: " +
                    exc.getMessage());
            System.exit(-1);
        }

        if (!cmdline.hasOption(INPUT_ARG) || !cmdline.hasOption(OUTPUT_ARG)) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(
                    FreebaseNameIndexBuilder.class.getCanonicalName(), options);
            System.exit(-1);
        }

        processFreebaseDump(cmdline.getOptionValue(INPUT_ARG),
                cmdline.getOptionValue(OUTPUT_ARG));
    }
}
