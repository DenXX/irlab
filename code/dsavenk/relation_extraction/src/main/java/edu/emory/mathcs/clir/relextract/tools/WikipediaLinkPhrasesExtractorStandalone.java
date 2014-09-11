package edu.emory.mathcs.clir.relextract.tools;

import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import edu.umd.cloud9.collection.wikipedia.WikipediaPagesBz2InputStream;
import edu.umd.cloud9.collection.wikipedia.language.WikipediaPageFactory;
import org.apache.commons.cli.*;

import java.util.HashMap;
import java.util.Map;

/**
 * A tool to extract links anchor phrases and target pages from the compressed
 * Wikipedia dump file.
 */
public class WikipediaLinkPhrasesExtractorStandalone {

    public static void processWikipediaDump(String input_path) throws Exception {
        HashMap<String, HashMap<String, Long>> anchorTargetCounts =
                new HashMap<String, HashMap<String, Long>>();
        WikipediaPage page = WikipediaPageFactory.createWikipediaPage("en");
        WikipediaPagesBz2InputStream stream =
                new WikipediaPagesBz2InputStream(input_path);
        while (stream.readNext(page)) {
            for (WikipediaPage.Link link : page.extractLinks()) {
                String anchor =  NlpUtils.normalizeStringForMatch(
                        link.getAnchorText());
                String target = NlpUtils.normalizeStringForMatch(
                        link.getTarget());

                if (!anchorTargetCounts.containsKey(anchor)) {
                    anchorTargetCounts.put(anchor,
                            new HashMap<String, Long>());
                }
                if (!anchorTargetCounts.get(anchor).containsKey(target)) {
                    anchorTargetCounts.get(anchor).put(target, 1L);
                } else {
                    anchorTargetCounts.get(anchor).put(target,
                            anchorTargetCounts.get(anchor).get(target) + 1L);
                }
            }
        }

        for (String anchor : anchorTargetCounts.keySet()) {
            long bestCount = 0;
            String bestTarget = "";
            for (Map.Entry<String, Long> targetCount :
                    anchorTargetCounts.get(anchor).entrySet()) {
                if (targetCount.getValue() > bestCount) {
                    bestCount = targetCount.getValue();
                    bestTarget = targetCount.getKey();
                }
            }
            System.out.println(anchor + "\t" + bestTarget);
        }
    }

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
        processWikipediaDump(input_path);
    }
}
