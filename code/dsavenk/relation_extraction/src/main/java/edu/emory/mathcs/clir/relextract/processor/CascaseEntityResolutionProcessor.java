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
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 11/10/14.
 */
public class CascaseEntityResolutionProcessor extends Processor {

    public static final String WIKILINKS_DICTIONARY_PARAMETER = "wikilinks_dict";
    public static final String WIKILINKS_LNRM_DICTIONARY_PARAMETER = "wikilinks_lnrm_dict";

    private static final double PHRASE_PROBABILITY_THRESHOLD = 0.0;

    private static final int MAX_PHRASE_IDS = 10;

    // TODO(denxx): Two more parameters are defined in the Lucene-based linker.
    private final Map<String, List<Pair<String, Float>>> wikilinksDictionary;
    private final Map<String, List<Pair<String, Float>>> wikilinksLnrmDictionary;
    private final Map<String, List<Pair<String, Float>>> searchCache_ = new ConcurrentHashMap<>();
    private final Map<String, List<Pair<String, Float>>> spellCheckSearchCache_ = new ConcurrentHashMap<>();
    private final SpellChecker spellChecker_;
    private final IndexSearcher searcher_;
    private List<Pair<String, Float>> emptyList_ = new ArrayList<>();
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

        System.err.println("Starting reading wikilinks dictionary...");
        readDictionary(properties.getProperty(WIKILINKS_DICTIONARY_PARAMETER),
                wikilinksDictionary);
        System.err.println("Starting reading normalized wikilinks dictionary...");
        readDictionary(properties.getProperty(WIKILINKS_LNRM_DICTIONARY_PARAMETER),
                wikilinksLnrmDictionary);
        System.err.println("Finished reading dictionaries.");

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
                        List<Pair<String, Float>> matches = resolveEntity(name, isOtherMention);

                        if (!matches.isEmpty()) {
                            mention.setEntityId(matches.get(0).first);

                            Map<String, Float> curMap =
                                    ("ENTITY".equals(mention.getType()))
                                    ? namedEntityIdScores
                                    : entityIdScores;

                            for (Pair<String, Float> match : matches) {
                                mention.addCandidateEntityId(match.first);
                                mention.addCandidateEntityScore(match.second);

                                if (!curMap.containsKey(match.first)) {
                                    curMap.put(match.first, match.second);
                                } else {
                                    // TODO(denxx): This is bad. We have 2 types of scores: p(entity|phrase) and triple count.
                                    curMap.put(match.first, Math.max(curMap.get(match.first), match.second));
                                }
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
                    if (!e.getKey().equals(bestId)) {
                        span.addCandidateEntityId(e.getKey());
                        span.addCandidateEntityScore(e.getValue());
                    }
                }
                for (Map.Entry<String, Float> e : entityIdScores.entrySet()) {
                    if (!e.getKey().equals(bestId)) {
                        span.addCandidateEntityId(e.getKey());
                        span.addCandidateEntityScore(e.getValue());
                    }
                }

            }
        }
        return docBuilder.build();
    }

    private List<Pair<String, Float>> resolveEntity(String name, boolean isOtherMention) {
        List<Pair<String, Float>> match = resolveByLinkPhrasesMatch(name);
        if (match.isEmpty()) {
            match = resolveByNormalizedPhrasesMatch(name);
            if (match.isEmpty() && !isOtherMention) {
                match = resolveByEntityNameCached(name);
                if (match.isEmpty()) {
                    match = resolveBySpellcorrectedEntityNameCached(name);
                }
            }
        }
        // Let's try to remove the first article
        if (name.toLowerCase().startsWith("the ") ||
                name.toLowerCase().startsWith("a ")) {
            List<Pair<String, Float>> match2 = resolveEntity(name.replaceFirst("^(?i)(the |a )", ""), isOtherMention);
            if (!match2.isEmpty()) {
                if (match.isEmpty()) {
                    match = match2;
                } else {
                    Set<String> has = match.stream().map(
                            e -> e.first).collect(Collectors.toSet());
                    match.addAll(match2.stream()
                            .filter(e -> has.contains(e.first))
                            .sorted((o1, o2) -> o2.second.compareTo(o1.second))
                            .collect(Collectors.toList()));
                }
            }
        }
        return match;
    }

    private List<Pair<String, Float>> resolveByLinkPhrasesMatch(String name) {
        if (wikilinksDictionary.containsKey(name)) {
            return wikilinksDictionary.get(name);
        }
        return emptyList_;
    }

    private List<Pair<String, Float>> resolveByNormalizedPhrasesMatch(String name) {
        String normalizedName = NlpUtils.normalizeStringForMatch(name);
        if (wikilinksLnrmDictionary.containsKey(normalizedName)) {
            return wikilinksLnrmDictionary.get(normalizedName);
        }
        return emptyList_;
    }

    private List<Pair<String, Float>> resolveByEntityNameCached(String name) {
        if (searchCache_.containsKey(name)) {
            return searchCache_.get(name);
        }

        List<Pair<String, Float>> res = resolveByEntityName(name, 0.8f, true);
        searchCache_.put(name, res);
        return res;
    }

    private List<Pair<String, Float>> resolveByEntityName(String name, float stopDocScoreDiff, boolean checkWordsCount) {
        ScoreDoc[] docs = new ScoreDoc[0];
        Query q = queryBuilder_.createMinShouldMatchQuery("name", name, 1.0f);
        // This can happen if query doesn't really contain any terms.
        if (q == null) return emptyList_;

        try {
            docs = searcher_.search(q, 100).scoreDocs;
        } catch (IOException e) {
            e.printStackTrace();
        }

        Map<String, Long> counts = new HashMap<>();
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
                counts.put(id, count);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (counts.isEmpty()) {
            List<Pair<String, Float>> res = counts.entrySet().stream()
                    .map(e -> new Pair<>(e.getKey(), (float) e.getValue()))
                    .collect(Collectors.toList());
            Collections.sort(res, (o1, o2) -> o2.second.compareTo(o1.second));
            return res;
        }
        return emptyList_;
    }

    private List<Pair<String, Float>> resolveBySpellcorrectedEntityNameCached(String name) {
        if (spellCheckSearchCache_.containsKey(name)) {
            return spellCheckSearchCache_.get(name);
        }
        List<Pair<String, Float>> res = resolveBySpellcorrectedEntityName(name);
        spellCheckSearchCache_.put(name, res);
        return res;
    }

    private List<Pair<String, Float>> resolveBySpellcorrectedEntityName(String name) {
        List<Pair<String, Float>> results = new ArrayList<>();
        try {
            for (String suggest : spellChecker_.suggestSimilar(name, 10, 0.8f)) {
                List<Pair<String, Float>> res = resolveByEntityName(suggest, 1, false);
                results.addAll(res.stream().collect(Collectors.toList()));
            }
            Collections.sort(results, (o1, o2) -> o2.second.compareTo(o1.second));
            return results;
        } catch (IOException e) {
            return emptyList_;
        }
    }

    private void readDictionary(
            String dictFileName,
            Map<String, List<Pair<String, Float>>> dictionary) throws IOException {

        BufferedReader input = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new FileInputStream(dictFileName))));
        String line;
        String lastPhrase = null;
        List<Pair<String, Float>> scores = null;
        while ((line = input.readLine()) != null) {
            String[] fields = line.split("\t");
            if (!fields[0].equals(lastPhrase)) {
                if (scores != null && !scores.isEmpty()) {
                    scores.sort((e1, e2) -> e2.second.compareTo(e1.second));
                    if (scores.size() > MAX_PHRASE_IDS) {
                        scores.subList(MAX_PHRASE_IDS, scores.size()).clear();
                    }
                } else {
                    dictionary.remove(lastPhrase);
                }
                lastPhrase = fields[0];
                dictionary.put(lastPhrase, new ArrayList<>());
                scores = dictionary.get(lastPhrase);
            }
            float score = Float.parseFloat(fields[2]);
            if (score >= PHRASE_PROBABILITY_THRESHOLD) {
                scores.add(new Pair<>(fields[1], score));
            }
        }
        // Put the last record to the dictionary.
        if (scores != null) {
            if (scores != null && !scores.isEmpty()) {
                scores.sort((e1, e2) -> e2.second.compareTo(e1.second));
                if (scores.size() > MAX_PHRASE_IDS) {
                    scores.subList(MAX_PHRASE_IDS, scores.size()).clear();
                }
            } else {
                dictionary.remove(lastPhrase);
            }
        }
    }
}
