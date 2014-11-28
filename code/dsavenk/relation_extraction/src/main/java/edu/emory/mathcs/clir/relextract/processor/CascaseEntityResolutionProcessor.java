package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.Pair;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 11/10/14.
 */
public class CascaseEntityResolutionProcessor extends Processor {

    public static final String WIKILINKS_DICTIONARY_PARAMETER = "wikilinks_dict";
    public static final String WIKILINKS_LNRM_DICTIONARY_PARAMETER = "wikilinks_lnrm_dict";

    private static final double PHRASE_PROBABILITY_THRESHOLD = 0.5;

    // TODO(denxx): Two more parameters are defined in the Lucene-based linker.
    private final Map<String, Pair<String, Float>> wikilinksDictionary;
    private final Map<String, Pair<String, Float>> wikilinksLnrmDictionary;
    private final Map<String, Pair<String, Float>> searchCache_ = new ConcurrentHashMap<>();
    private final Map<String, Pair<String, Float>> spellCheckSearchCache_ = new ConcurrentHashMap<>();
    private final Pair<String, Float> emptyPair = new Pair<>();
    private final SpellChecker spellChecker_;
    private final IndexSearcher searcher_;
    // Counters for
    private AtomicInteger total = new AtomicInteger(0);
    private AtomicInteger resolved = new AtomicInteger(0);
    private QueryBuilder queryBuilder_ = new QueryBuilder(
            new StandardAnalyzer(new CharArraySet(0, true)));

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public CascaseEntityResolutionProcessor(Properties properties) throws IOException {
        super(properties);
        wikilinksDictionary = new HashMap<>();
        wikilinksLnrmDictionary = new HashMap<>();

        readDictionary(properties.getProperty(WIKILINKS_DICTIONARY_PARAMETER),
                wikilinksDictionary);
        readDictionary(properties.getProperty(WIKILINKS_LNRM_DICTIONARY_PARAMETER),
                wikilinksLnrmDictionary);

        Directory spellIndexDir = FSDirectory.open(
                new File(properties.getProperty(
                        LuceneEntityResolutionProcessor.LUCENE_SPELLCHECKINDEX_PARAMETER)));
        Directory searchIndexDir = FSDirectory.open(
                new File(properties.getProperty(
                        LuceneEntityResolutionProcessor.LUCENE_INDEX_PARAMETER)));
        IndexReader searchIndexReader = DirectoryReader.open(searchIndexDir);
        spellChecker_ = new SpellChecker(spellIndexDir);
        searcher_ = new IndexSearcher(searchIndexReader);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws Exception {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();

        for (Document.Span.Builder span : docBuilder.getSpanBuilderList()) {
            span.clearCandidateEntityId();
            span.clearCandidateEntityScore();
            span.clearEntityId();
            // Do not try resolve measures.
            boolean isNamedEntity = "ENTITY".equals(span.getType());
            boolean isOtherEntity = "OTHER".equals(span.getType());
            if (isNamedEntity || isOtherEntity) {
                total.incrementAndGet();

                Map<String, Float> namedEntityIdScores = new HashMap<>();
                Map<String, Float> entityIdScores = new HashMap<>();

                // Iterate over all mentions.
                for (Document.Mention.Builder mention : span.getMentionBuilderList()) {
                    mention.clearEntityId();
                    mention.clearCandidateEntityId();
                    mention.clearCandidateEntityScore();

                    if (!mention.getMentionType().equals("PRONOMINAL") &&
                            !mention.getText().toLowerCase().equals("this") &&
                            !mention.getText().toLowerCase().equals("that")) {
                        String name = PTBTokenizer.ptb2Text(mention.getValue());
                        boolean isOtherMention = mention.getType().equals("OTHER");
                        Pair<String, Float> match = resolveEntity(name, isOtherMention);

                        if (match != emptyPair) {
                            mention.setEntityId(match.first);
                            mention.addCandidateEntityId(match.first);
                            mention.addCandidateEntityScore(match.second);
                            Map<String, Float> curMap = null;
                            if ("ENTITY".equals(mention.getType())) {
                                curMap = namedEntityIdScores;
                            } else {
                                curMap = entityIdScores;
                            }

                            if (!curMap.containsKey(match.first)) {
                                curMap.put(match.first, match.second);
                            } else {
                                // TODO(denxx): This is bad. We have 2 types of scores: p(entity|phrase) and triple count.
                                curMap.put(match.first, Math.max(curMap.get(match.first), match.second));
                            }
                        }
                    }
                }

                Map<String, Float> curMap;
                if (namedEntityIdScores.size() > 0) {
                    curMap = namedEntityIdScores;
                } else {
                    curMap = entityIdScores;
                }

                String bestId = "";
                float bestScore = -1f;
                for (Map.Entry<String, Float> e : curMap.entrySet()) {
                    if (e.getValue() > bestScore) {
                        bestId = e.getKey();
                        bestScore = e.getValue();
                    }
                }
                if (!bestId.isEmpty()) {
                    resolved.incrementAndGet();

                    span.setEntityId(bestId);
                    span.addCandidateEntityId(bestId);
                    span.addCandidateEntityScore(bestScore);
                }

                for (Map.Entry<String, Float> e : namedEntityIdScores.entrySet()) {
                    span.addCandidateEntityId(e.getKey());
                    span.addCandidateEntityScore(e.getValue());
                }
                for (Map.Entry<String, Float> e : entityIdScores.entrySet()) {
                    span.addCandidateEntityId(e.getKey());
                    span.addCandidateEntityScore(e.getValue());
                }

            }
        }
        return docBuilder.build();
    }

    private Pair<String, Float> resolveEntity(String name, boolean isOtherMention) {
        Pair<String, Float> match = resolveByLinkPhrasesMatch(name);
        if (match == emptyPair) {
            match = resolveByNormalizedPhrasesMatch(name);
            if (match == emptyPair) { // && !isOtherMention) {
                match = resolveByEntityNameCached(name);
                if (match == emptyPair) {
                    match = resolveBySpellcorrectedEntityNameCached(name);
                }
            }
        }
        // Let's try to remove the first article
        if (name.toLowerCase().startsWith("the ") ||
                name.toLowerCase().startsWith("a ")) {
            Pair<String, Float> match2 = resolveEntity(name.replaceFirst("^(?i)(the |a )", ""), isOtherMention);
            if (match2 != emptyPair) {
                if (match == emptyPair) {
                    match = match2;
                } else {
                    if (match.second < match2.second) {
                        match = match2;
                    }
                }
            }
        }
        return match;
    }

    private Pair<String, Float> resolveByLinkPhrasesMatch(String name) {
        if (wikilinksDictionary.containsKey(name)) {
            return wikilinksDictionary.get(name);
        }
        return emptyPair;
    }

    private Pair<String, Float> resolveByNormalizedPhrasesMatch(String name) {
        String normalizedName = NlpUtils.normalizeStringForMatch(name);
        if (wikilinksLnrmDictionary.containsKey(normalizedName)) {
            return wikilinksLnrmDictionary.get(normalizedName);
        }
        return emptyPair;
    }

    private Pair<String, Float> resolveByEntityNameCached(String name) {
        if (searchCache_.containsKey(name)) {
            return searchCache_.get(name);
        }

        Pair<String, Float> res = resolveByEntityName(name, 0.8f, true);
        searchCache_.put(name, res);
        return res;
    }

    private Pair<String, Float> resolveByEntityName(String name, float stopDocScoreDiff, boolean checkWordsCount) {
        ScoreDoc[] docs = new ScoreDoc[0];
        Query q = queryBuilder_.createMinShouldMatchQuery("name", name, 1.0f);
        // This can happen if query doesn't really contain any terms.
        if (q == null) return emptyPair;

        try {
            docs = searcher_.search(q, 100).scoreDocs;
        } catch (IOException e) {
            e.printStackTrace();
        }

        String bestId = "";
        long bestCount = 0;
        for (ScoreDoc doc : docs) {
            try {
                org.apache.lucene.document.Document document =
                        searcher_.doc(doc.doc);

                if (doc.score < docs[0].score * stopDocScoreDiff ||
                        (checkWordsCount &&
                                document.get("name").split("\\s+").length >
                                        name.split("\\s+").length + 1)) {
                    break;
                }
                long count = Long.parseLong(document.get("triple_count"));
                String id = document.get("id");
                if (count > bestCount) {
                    bestId = id;
                    bestCount = count;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (!bestId.isEmpty()) {
            Pair<String, Float> res = new Pair<>(bestId, (float) bestCount);
            return res;
        }
        return emptyPair;
    }

    private Pair<String, Float> resolveBySpellcorrectedEntityNameCached(String name) {
        if (spellCheckSearchCache_.containsKey(name)) {
            return spellCheckSearchCache_.get(name);
        }
        Pair<String, Float> res = resolveBySpellcorrectedEntityName(name);
        spellCheckSearchCache_.put(name, res);
        return res;
    }

    private Pair<String, Float> resolveBySpellcorrectedEntityName(String name) {
        try {
            Pair<String, Float> bestPair = new Pair<>("", 0f);
            for (String suggest : spellChecker_.suggestSimilar(name, 10, 0.8f)) {
                Pair<String, Float> res = resolveByEntityName(suggest, 1, false);
                if (res != emptyPair && res.second > bestPair.second) {
                    bestPair = res;
                }
            }
            if (bestPair.second > 0) {
                return bestPair;
            }
            return emptyPair;
        } catch (IOException e) {
            return emptyPair;
        }
    }

    private void readDictionary(
            String dictFileName,
            Map<String, Pair<String, Float>> dictionary) throws IOException {

        BufferedReader input = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new FileInputStream(dictFileName))));
        String line;
        String lastPhrase = "";
        String bestEntity = null;
        float bestScore = -1;
        while ((line = input.readLine()) != null) {
            String[] fields = line.split("\t");
            if (!fields[0].equals(lastPhrase) && bestEntity != null) {
                dictionary.put(lastPhrase, new Pair<>(bestEntity, bestScore));
                bestEntity = null;
                bestScore = -1;
                lastPhrase = fields[0];
            }
            float score = Float.parseFloat(fields[2]);
            if (score > PHRASE_PROBABILITY_THRESHOLD && score > bestScore) {
                bestScore = score;
                bestEntity = fields[1];
            }
        }
        // Put the last record to the dictionary.
        if (bestEntity != null) {
            dictionary.put(lastPhrase, new Pair<>(bestEntity, bestScore));
        }
    }
}
