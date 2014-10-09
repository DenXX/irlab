package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import org.apache.commons.collections4.trie.PatriciaTrie;

import java.io.*;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * A processor, that uses a phrase->id lexicon to resolve named entities in
 * the documents to Freebase.
 */
public class EntityResolutionProcessor extends Processor {

    /**
     * Name of the property storing the location of the entity lexicon.
     */
    public static final String LEXICON_PARAMETER = "entityres_lexicon";

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public EntityResolutionProcessor(Properties properties) throws IOException {
        super(properties);
        String lexicon_filename = properties.getProperty(LEXICON_PARAMETER);
        System.err.println("Loading entity names ...");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(
                        new FileInputStream(lexicon_filename))));
        String line = null;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            int langPos = fields[0].indexOf("\"@", 1);
            String name = fields[0].substring(1, langPos);
            String lang = fields[0].substring(langPos + 2);
            // Currently we only use English names and aliases.
            if (lang.equals("en")) {
                long bestCount = 0;
                String bestEntity = null;
                for (int i = 2; i < fields.length; i += 2) {
                    long count = Long.parseLong(fields[i]);
                    if (count > bestCount) {
                        bestCount = count;
                        bestEntity = fields[i - 1];
                    }
                }
                // We should have at least one entity and count shouldn't be 0.
                assert bestEntity != null;
                final String normalizedName =
                        NlpUtils.normalizeStringForMatch(name);
                namesIndex_.put(normalizedName, bestEntity);
            }
        }
        System.err.println("Done loading entity names.");
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws Exception {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        for (Document.Span.Builder span : docBuilder.getSpanBuilderList()) {
            if ("PERSON".equals(span.getType()) ||
                "LOCATION".equals(span.getType()) ||
                "ORGANIZATION".equals(span.getType()) ||
                "MISC".equals(span.getType())) {
                total.incrementAndGet();
                final String normalizedName = NlpUtils.normalizeStringForMatch(
                        span.getValue());
                if (namesIndex_.containsKey(normalizedName)) {
                    resolved.incrementAndGet();
                    final String entityId = namesIndex_.get(normalizedName);
                    span.setEntityId(entityId);
                }
            }
        }
        return docBuilder.build();
    }

    @Override
    public void finishProcessing() {
        System.err.println("Total: " + total.get());
        System.err.println("Resolved: " + resolved.get());
    }

    // A trie, that maps a name to the entity with the best score from the
    // list of available entities. Currently the score is the number of triples
    // available for the entity, so we prefer more "popular" entities.
    private final PatriciaTrie<String> namesIndex_ = new PatriciaTrie<>();
    private final AtomicInteger resolved = new AtomicInteger(0);
    private final AtomicInteger total = new AtomicInteger(0);

}
