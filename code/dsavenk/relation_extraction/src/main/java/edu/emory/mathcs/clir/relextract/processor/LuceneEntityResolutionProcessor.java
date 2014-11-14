package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.process.PTBTokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dsavenk on 10/20/14.
 */
public class LuceneEntityResolutionProcessor extends Processor {

    public static final String LUCENE_INDEX_PARAMETER = "lucene_lexicon_index";
    public static final String LUCENE_SPELLCHECKINDEX_PARAMETER = "spellcheck_index";
    private final ConcurrentMap<String, String[]> entityNameCache = new ConcurrentHashMap<>();
    private final IndexSearcher searcher_;
    private final SpellChecker spellChecker_;
    private final AtomicInteger resolved = new AtomicInteger(0);
    private final AtomicInteger total = new AtomicInteger(0);
    QueryBuilder queryBuilder_ = new QueryBuilder(new StandardAnalyzer(
            new CharArraySet(0, true)));

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
                new File(properties.getProperty(
                        LUCENE_SPELLCHECKINDEX_PARAMETER)));
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
            if ("ENTITY".equals(span.getType()) ||
                    "OTHER".equals(span.getType())) {
                Set<String> entityIds = new HashSet<>();
                String longestMentionEntityId = "";
                int longestMentionLength = -1;
                total.incrementAndGet();
                String name;
                for (Document.Mention.Builder mention :
                        span.getMentionBuilderList()) {
                    if (mention.getMentionType().equals("NOMINAL") ||
                            mention.getMentionType().equals("PROPER")) {
                        name = PTBTokenizer.ptb2Text(mention.getValue());
                        // Do not try to correct spelling for mentions that are
                        // too long.
                        String[] entityId = resolveEntity(name,
                                (mention.getTokenEndOffset() -
                                        mention.getTokenBeginOffset()) < 3 &&
                                        span.getType().equals("ENTITY"));
                        if (entityId.length > 0) {
                            mention.setEntityId(entityId[0]);
                            for (String id : entityId) {
                                mention.addCandidateEntityId(id);
                                entityIds.add(id);
                            }
                            if (name.length() > longestMentionLength) {
                                longestMentionEntityId = entityId[0];
                                longestMentionLength = name.length();
                            }
                        }
                    }
                }
                if (longestMentionLength != -1) {
                    resolved.incrementAndGet();
                    span.setEntityId(longestMentionEntityId);
                    for (String id : entityIds) {
                        span.addCandidateEntityId(id);
                    }
                }
            }
        }
        return docBuilder.build();
    }

    private String[] resolveEntity(final String name, boolean correctSpelling) {
        // Check cache.
        if (entityNameCache.containsKey(name))
            return entityNameCache.get(name);

        String res = "";
        ScoreDoc[] docs = new ScoreDoc[0];
        Query q = queryBuilder_.createMinShouldMatchQuery("name", name, 1.0f);
        // This can happen if query doesn't really contain any terms.
        if (q == null) return new String[0];

        try {
            docs = searcher_.search(q, 100).scoreDocs;
        } catch (IOException e) {
            e.printStackTrace();
        }

        long maxCount = 0;
        long maxPhraseCount = -1;
        Set<String> candidateIds = new HashSet<>();
        for (ScoreDoc doc : docs) {
            try {
                org.apache.lucene.document.Document document =
                        searcher_.doc(doc.doc);
                // Quit if the number of terms in the entity name is more than
                // twice the number of tokens in our name.
                if (doc.score < docs[0].score * 0.8 ||
                        document.get("name").split("\\s+").length >
                                name.split("\\s+").length + 1) {
                    break;
                }
                long count = Long.parseLong(document.get("triple_count"));
                long phraseCount = -1; //Long.parseLong(document.get("phrase_count"));
                String id = document.get("id");
                candidateIds.add(id);
                if ((phraseCount != -1 && phraseCount > maxPhraseCount) ||
                        (maxPhraseCount == -1 && count > maxCount)) {
                    res = id;
                    maxCount = count;
                    maxPhraseCount = phraseCount;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (correctSpelling && res.isEmpty() && name.length() < 255) {
            maxCount = 0;
            String[] suggestions = new String[0];
            try {
                suggestions = spellChecker_.suggestSimilar(name, 10, 0.8f);
            } catch (IOException e) {
                e.printStackTrace();
            }
            for (String suggest : suggestions) {
                docs = new ScoreDoc[0];
                q = queryBuilder_.createMinShouldMatchQuery("name", suggest, 1.0f);
                try {
                    docs = searcher_.search(q, 10).scoreDocs;
                } catch (IOException e) {
                    e.printStackTrace();
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
                    if (doc.score < docs[0].score ||
                            !document.get("name").equals(suggest)) {
                        break;
                    }
                    String id = document.get("id");
                    long count = Long.parseLong(document.get("triple_count"));
                    long phraseCount = Long.parseLong(document.get("phrase_count"));
                    candidateIds.add(id);
                    if ((phraseCount != -1 && phraseCount > maxPhraseCount) ||
                            (maxPhraseCount == -1 && count > maxCount)) {
                        res = id;
                        maxCount = count;
                        maxPhraseCount = phraseCount;
                    }
                }
            }
        }
        String[] resList;
        if (!res.isEmpty()) {
            resList = new String[candidateIds.size()];
            resList[0] = res;
            int index = 1;
            for (String id : candidateIds) {
                if (!id.equals(res)) {
                    resList[index++] = id;
                }
            }
            entityNameCache.put(name, resList);
            return resList;
        }
        resList = new String[0];
        entityNameCache.put(name, resList);
        return resList;
    }

    @Override
    public void finishProcessing() {
        System.err.println("Total: " + total.get());
        System.err.println("Resolved: " + resolved.get());
    }

}
