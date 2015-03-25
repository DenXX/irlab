package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.Pair;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.*;
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

    private static final int MAX_PHRASE_IDS = 5;

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
    private Sort sort_ = new Sort(SortField.FIELD_SCORE,
            new SortField("triple_count", SortField.Type.LONG, true));
    private boolean alwaysLookupName = true;

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
            if (span.getNerType().equals("NUMBER") ||
                    span.getNerType().equals("ORDINAL")) {
                span.setType("MEASURE");
            }

            boolean isNamedEntity = "ENTITY".equals(span.getType());
            boolean isOtherEntity = "OTHER".equals(span.getType());
            if (isNamedEntity || isOtherEntity) {
                total.incrementAndGet();

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

                            for (Pair<String, Float> match : matches) {
                                mention.addCandidateEntityId(match.first);
                                mention.addCandidateEntityScore(match.second);

                                if (!entityIdScores.containsKey(match.first)) {
                                    entityIdScores.put(match.first, match.second);
                                } else {
                                    // TODO(denxx): This is bad. We have 2 types of scores: p(entity|phrase) and triple count.
                                    entityIdScores.put(match.first,
                                            Math.max(entityIdScores.get(match.first), match.second));
                                }
                            }
                        }
                    }
                }

                String bestId = "";
                float bestScore = -1f;
                for (Map.Entry<String, Float> e : entityIdScores.entrySet()) {
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

                List<Map.Entry<String, Float>> ent = new ArrayList<>(entityIdScores.entrySet());
                ent.sort((e1, e2) -> e2.getValue().compareTo(e1.getValue()));
                for (Map.Entry<String, Float> e : ent) {
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
        Map<String, Float> matches = new HashMap<>();

        if (!isOtherMention || alwaysLookupName) {
            addMatches(matches, resolveByEntityNameCached(name));
        }

        addMatches(matches, resolveByLinkPhrasesMatch(name));
        if (matches.isEmpty()) {
            addMatches(matches, resolveByNormalizedPhrasesMatch(name));
            if (!isOtherMention && matches.isEmpty()) {
                addMatches(matches, resolveBySpellcorrectedEntityNameCached(name));
            }
        }

        // Let's try to remove the/a in front of entity.
        if (matches.size() < MAX_PHRASE_IDS &&
                name.toLowerCase().startsWith("the ") ||
                name.toLowerCase().startsWith("a ")) {
            addMatches(matches, resolveEntity(name.replaceFirst("^(?i)(the |a )", ""), isOtherMention));
        }
        return matches.entrySet().stream()
                .map(entry -> new Pair<>(entry.getKey(), entry.getValue()))
                .sorted((p1, p2) -> p2.second.compareTo(p1.second))
                .collect(Collectors.toList());
    }

    private void addMatches(Map<String, Float> matches, List<Pair<String, Float>> foundMatches) {
        if (foundMatches != null) {
            for (Pair<String, Float> match : foundMatches) {
                matches.put(match.first,
                        Math.max(match.second,
                                matches.containsKey(match.first)
                                        ? matches.get(match.first)
                                        : Float.NEGATIVE_INFINITY));
            }
        }
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

    private List<Pair<String, Float>> resolveByEntityName(
            String name, float stopDocScoreDiff, boolean checkWordsCount) {
        ScoreDoc[] docs = new ScoreDoc[0];
        Query q = queryBuilder_.createMinShouldMatchQuery("name", name, 1.0f);
        // This can happen if query doesn't really contain any terms.
        if (q == null) return emptyList_;

        TopDocs topDocs = null;
        try {
            topDocs = searcher_.search(q, MAX_PHRASE_IDS, sort_);
            docs = topDocs.scoreDocs;
        } catch (IOException e) {
            e.printStackTrace();
        }

        List<Pair<String, Float>> counts = new ArrayList<>();
        long maxCount = 1;
        float maxScore = (docs.length > 0) ? (float)((FieldDoc)docs[0]).fields[0] : 0;
        for (ScoreDoc doc : docs) {
            try {
                org.apache.lucene.document.Document document =
                        searcher_.doc(doc.doc);

                float score = (float)((FieldDoc)doc).fields[0];
                if (score < maxScore * stopDocScoreDiff ||
                        (checkWordsCount &&
                                1.0 * document.get("name").split("\\s+").length /
                                        name.split("\\s+").length < 0.6)) {
                    break;
                }
                long count = (long)((FieldDoc)doc).fields[1];
                maxCount = Math.max(maxCount, count);
                String id = document.get("id");
                counts.add(new Pair<>(id, (float)count));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        for (Pair<String, Float> p : counts) {
            p.second = 1.0f * p.second / maxCount;
        }
        return counts;
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
            results.sort((o1, o2) -> o2.second.compareTo(o1.second));
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
            if (score >= Parameters.MIN_ENTITYID_SCORE) {
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
