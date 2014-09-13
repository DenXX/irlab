package edu.emory.mathcs.clir.relextract.tools;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.GnuParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;

import java.io.*;
import java.util.*;

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

    private static String stripFreebaseRdfNamespace(String rdfUri) {
        return rdfUri.replace("<http://rdf.freebase.com/ns", "")
                .replace(">", "").replace(".", "/");
    }

    private static void processFreebaseDump(final String freebase_dump_file,
                                            String outputFile) {
        HashMap<String, Long> entityTriplesCount = new HashMap<String, Long>();
        HashMap<String, Set<String>> nameEntityIndex =
                new HashMap<String, Set<String>>();

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new InputStreamReader(
                    new CompressorStreamFactory().createCompressorInputStream(
                            CompressorStreamFactory.GZIP,
                            new BufferedInputStream(
                                    new FileInputStream(
                                            freebase_dump_file)))));
        } catch (CompressorException e) {
            System.err.println("Cannot decompress file: " +
                    e.getMessage());
            System.exit(-1);
        } catch (FileNotFoundException e) {
            System.err.println("File not found: " + freebase_dump_file);
            System.exit(-1);
        }

        String line;
        try {
            while ((line = reader.readLine()) != null) {
                String[] triple = line.split("\t");
                String subject = stripFreebaseRdfNamespace(triple[0]);
                String object = stripFreebaseRdfNamespace(triple[2]);
                String predicate = stripFreebaseRdfNamespace(triple[1]);

                if (subject.startsWith("/m/")) {
                    if (!entityTriplesCount.containsKey(subject)) {
                        entityTriplesCount.put(subject, 1L);
                    } else {
                        entityTriplesCount.put(subject,
                                entityTriplesCount.get(subject) + 1L);
                    }

                    if (predicate.equals("/type/object/name") ||
                                    predicate.equals("/common/topic/alias")) {
                        if (!nameEntityIndex.containsKey(object)) {
                            nameEntityIndex.put(object, new HashSet<String>());
                        }
                        nameEntityIndex.get(object).add(subject);
                    }
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(-1);
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
