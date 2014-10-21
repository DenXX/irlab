package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dsavenk on 10/20/14.
 */
public class LuceneEntityResolutionProcessor extends Processor {

    public static final String LUCENE_INDEX_PARAMETER = "lucene_lexicon_index";
    public static final String LUCENE_SPELLCHECKINDEX_PARAMETER = "spellcheck_index";
    private final ConcurrentMap<String, String> entityNameCache = new ConcurrentHashMap<>();
    private final IndexSearcher searcher_;
    private final SpellChecker spellChecker_;
    private final AtomicInteger resolved = new AtomicInteger(0);
    private final AtomicInteger total = new AtomicInteger(0);

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public LuceneEntityResolutionProcessor(Properties properties) throws IOException, ParseException {
        super(properties);
        Directory spellIndexDir = FSDirectory.open(
                new File(properties.getProperty(LUCENE_SPELLCHECKINDEX_PARAMETER)));
        Directory searchIndexDir = FSDirectory.open(
                new File(properties.getProperty(LUCENE_INDEX_PARAMETER)));
        IndexReader searchIndexReader = DirectoryReader.open(searchIndexDir);
        spellChecker_ = new SpellChecker(spellIndexDir);
        searcher_ = new IndexSearcher(searchIndexReader);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        for (Document.Span.Builder span : docBuilder.getSpanBuilderList()) {
            if ("ENTITY".equals(span.getType())) {
                String longestMentionEntityId = "";
                int longestMentionLength = -1;
                total.incrementAndGet();
                String name = span.getValue();
                String entityId = resolveEntity(name);
                if (!entityId.isEmpty()) {
                    if (name.length() > longestMentionLength) {
                        longestMentionEntityId = entityId;
                        longestMentionLength = name.length();
                    }
                }
                // TODO(denxx): Decide on 2 strategies: longest mention or most
                // frequent.
                for (Document.Mention.Builder mention :
                        span.getMentionBuilderList()) {
                    if (mention.getMentionType().equals("NOMINAL") ||
                            mention.getMentionType().equals("PROPER")) {
                        name = mention.getValue();
                        entityId = resolveEntity(name);
                        if (!entityId.isEmpty()) {
                            mention.setEntityId(entityId);
                            if (name.length() > longestMentionLength) {
                                longestMentionEntityId = entityId;
                                longestMentionLength = name.length();
                            }
                        }
                    }
                }
                if (longestMentionLength != -1) {
                    resolved.incrementAndGet();
                    span.setEntityId(longestMentionEntityId);
                }
            }
        }
        return docBuilder.build();
    }

    private String resolveEntity(String name) {
        if (entityNameCache.containsKey(name))
            return entityNameCache.get(name);
        Analyzer analyzer = new StandardAnalyzer(new CharArraySet(0, true));
        QueryParser queryParser = new QueryParser("name", analyzer);
        String res = "";
        char[] nameChars = NlpUtils.normalizeStringForMatch(name).toCharArray();
        Arrays.sort(nameChars);
        ScoreDoc[] docs = new ScoreDoc[0];
        try {
            docs = searcher_.search(queryParser.parse(QueryParser.escape(name)),
                    1000).scoreDocs;
        } catch (Exception e) {
            e.printStackTrace();
        }

        long maxCount = 0;
        for (ScoreDoc doc : docs) {
            try {
                org.apache.lucene.document.Document document =
                        searcher_.doc(doc.doc);
                String entityName = document.get("name");
                char[] entityNameChars = NlpUtils.normalizeStringForMatch(entityName).toCharArray();
                Arrays.sort(entityNameChars);
                if (charSimilarity(entityNameChars, nameChars) < 0.9) {
                    break;
                }
                long count = Long.parseLong(document.get("triple_count"));
                if (count > maxCount) {
                    res = document.get("id");
                    maxCount = count;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (res.isEmpty()) {
            maxCount = 0;
            String[] suggestions = new String[0];
            try {
                suggestions = spellChecker_.suggestSimilar(name, 10, 0.8f);
            } catch (IOException e) {
            }
            for (String suggest : suggestions) {
                docs = new ScoreDoc[0];
                try {
                    docs = searcher_.search(
                            queryParser.parse(QueryParser.escape(suggest)),
                            1000).scoreDocs;
                } catch (Exception e) {
                }

                for (ScoreDoc doc : docs) {
                    org.apache.lucene.document.Document document = null;
                    try {
                        document = searcher_.doc(doc.doc);
                    } catch (IOException e) {
                        // Shouldn't happen, but just in case we will just
                        // continue loop.
                        continue;
                    }
                    String entityName = document.get("name");
                    char[] entityNameChars = NlpUtils.normalizeStringForMatch(entityName).toCharArray();
                    Arrays.sort(entityNameChars);
                    if (charSimilarity(entityNameChars, nameChars) < 0.8) {
                        break;
                    }
                    long count = Long.parseLong(document.get("triple_count"));
                    if (count > maxCount) {
                        res = document.get("id");
                        maxCount = count;
                    }
                }
            }
        }
        entityNameCache.put(name, res);
        return res;
    }

    private double charSimilarity(char[] entityNameChars, char[] nameChars) {
        int same = 0;
        int total = Math.max(entityNameChars.length, nameChars.length);
        int i = 0, j = 0;
        while (i < entityNameChars.length && j < nameChars.length) {
            if (entityNameChars[i] == nameChars[j]) {
                ++same;
                ++i;
                ++j;
            } else if (entityNameChars[i] < nameChars[j]) ++i;
            else ++j;
        }
        return 1.0 * same / total;
    }

    @Override
    public void finishProcessing() {
        System.err.println("Total: " + total.get());
        System.err.println("Resolved: " + resolved.get());
    }

}
