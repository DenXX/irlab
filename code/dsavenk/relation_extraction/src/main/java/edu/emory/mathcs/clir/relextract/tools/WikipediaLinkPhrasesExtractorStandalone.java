package edu.emory.mathcs.clir.relextract.tools;

import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import edu.umd.cloud9.collection.wikipedia.WikipediaPagesBz2InputStream;
import edu.umd.cloud9.collection.wikipedia.language.WikipediaPageFactory;
import org.apache.commons.cli.*;

/**
 * A tool to extract links anchor phrases and target pages from the compressed
 * Wikipedia dump file.
 */
public class WikipediaLinkPhrasesExtractorStandalone {
    public static void main(String[] args) throws Exception {
        Options options = new Options();
        options.addOption(OptionBuilder.withArgName("path").hasArg()
                .withDescription("bz2 wikipedia dump file").create("input"));

        CommandLine cmdline = null;
        CommandLineParser parser = new GnuParser();
        try {
            cmdline = parser.parse(options, args);
        } catch (ParseException exc) {
            System.err.println("Error parsing command line: " + exc.getMessage());
            System.exit(-1);
        }

        if (!cmdline.hasOption("input")) {
            HelpFormatter formatter = new HelpFormatter();
            formatter.printHelp(WikipediaLinkPhrasesExtractorStandalone.class
                            .getCanonicalName(), options);
            System.exit(-1);
        }

        String input_path = cmdline.getOptionValue("input");
        WikipediaPage page = WikipediaPageFactory.createWikipediaPage("en");
        WikipediaPagesBz2InputStream stream =
                new WikipediaPagesBz2InputStream(input_path);
        while (stream.readNext(page)) {
            for (WikipediaPage.Link link : page.extractLinks()) {
                System.out.println(link.getAnchorText() + "\t" +
                        link.getTarget());
            }
        }
    }
}
