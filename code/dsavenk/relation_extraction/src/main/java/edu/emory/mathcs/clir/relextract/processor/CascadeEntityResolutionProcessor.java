package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.entity.CacheEntityLinkingWrapper;
import edu.emory.mathcs.clir.entity.EntityLinking;
import edu.emory.mathcs.clir.entity.FreebaseDictEntityLinking;
import edu.emory.mathcs.clir.entity.WikiLinksDictionaryEntityLinking;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.util.Pair;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.QueryBuilder;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dsavenk on 11/10/14.
 */
public class CascadeEntityResolutionProcessor extends Processor {

    public static final String ENTITYRES_ALWAYS_FREEBASEDICT_PARAMETER = "entityres_alwaysfreebasedict";
    public static final String WIKILINKS_DICTIONARY_PARAMETER = "wikilinks_dict";
    public static final String WIKILINKS_LNRM_DICTIONARY_PARAMETER = "wikilinks_lnrm_dict";

    // TODO(denxx): Two more parameters are defined in the Lucene-based linker.
    private final EntityLinking wikiLinksEntityLinker_;
    private final EntityLinking freebaseEntityLinker_;

    private final Map<String, List<Pair<String, Float>>> searchCache_ = new ConcurrentHashMap<>();
    private final Map<String, List<Pair<String, Float>>> spellCheckSearchCache_ = new ConcurrentHashMap<>();
    private final SpellChecker spellChecker_;
    private List<Pair<String, Float>> emptyList_ = new ArrayList<>();
    // Counters for
    private AtomicInteger total = new AtomicInteger(0);
    private AtomicInteger resolved = new AtomicInteger(0);
    private QueryBuilder queryBuilder_ = new QueryBuilder(
            new StandardAnalyzer(new CharArraySet(0, true)));
    private Sort sort_ = new Sort(SortField.FIELD_SCORE,
            new SortField("triple_count", SortField.Type.LONG, true));
    private boolean alwaysLookupName = true;

    private final KnowledgeBase kb_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public CascadeEntityResolutionProcessor(Properties properties) throws IOException {
        super(properties);

        kb_ = KnowledgeBase.getInstance(properties);

        wikiLinksEntityLinker_ = new WikiLinksDictionaryEntityLinking(
                properties.getProperty(WIKILINKS_DICTIONARY_PARAMETER),
                properties.getProperty(WIKILINKS_LNRM_DICTIONARY_PARAMETER));
        freebaseEntityLinker_ = new CacheEntityLinkingWrapper(new FreebaseDictEntityLinking(
                KnowledgeBase.getInstance(properties)));

        Directory spellIndexDir = FSDirectory.open(
                new File(properties.getProperty(
                        LuceneEntityResolutionProcessor.LUCENE_SPELLCHECKINDEX_PARAMETER)));
        spellChecker_ = new SpellChecker(spellIndexDir);
        if (properties.containsKey(ENTITYRES_ALWAYS_FREEBASEDICT_PARAMETER)) {
            alwaysLookupName = true;
        }
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
                List<Document.Mention.Builder> mentions = new ArrayList(span.getMentionBuilderList());
                mentions.sort((o1, o2) -> {
                    if (o1 == o2) return 0;
                    if (span.hasRepresentativeMention()) {
                        if (span.getMentionBuilder(span.getRepresentativeMention()) == o1)
                            return -1;
                        if (span.getMentionBuilder(span.getRepresentativeMention()) == o2)
                            return 1;
                    }
                    if (o1.getText().length() == o2.getText().length()) return 0;
                    else return o1.getText().length() > o2.getText().length() ? -1 : 1;
                });

                for (Document.Mention.Builder mention : mentions) {
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

                            // If we found a match, break!
                            break;
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

        Map<String, Float> freebaseEntities = freebaseEntityLinker_.resolveEntity(name);
        Map<String, Float> wikiLinksEntities = wikiLinksEntityLinker_.resolveEntity(name);

        Set<String> mids = new HashSet<>();
        mids.addAll(freebaseEntities.keySet());
        mids.addAll(wikiLinksEntities.keySet());

        List<Pair<String, Float>> res = new ArrayList<>();
        for (String mid : mids) {
//            float triples = freebaseEntities.getOrDefault(mid, 0f);
//            float score = wikiLinksEntities.getOrDefault(mid, 0f);
//            String entityName = kb_.getEntityName(mid);
//            int match = 0;
//            int total = 0;
//            for (String token : entityName.split("\\s+")) {
//                if (name.toLowerCase().contains(token.toLowerCase())) match++;
//                ++total;
//            }
//            System.out.println(name + "\t" + mid + "\t" + entityName + "\t" + triples + "\t" + score + "\t" + kb_.getTripleCount(mid) + "\t" + (2.0 * match / (total + nameTokens)));
            long triplesCount = kb_.getTripleCount(mid);
            if (triplesCount > 42) {
                res.add(new Pair<>(mid, (float) triplesCount));
            }
        }


        // Let's try to remove the/a in front of entity.
//        if (matches.size() < MAX_PHRASE_IDS &&
//                name.toLowerCase().startsWith("the ") ||
//                name.toLowerCase().startsWith("a ")) {
//            addMatches(matches, resolveEntity(name.replaceFirst("^(?i)(the |a )", ""), isOtherMention));
//        }

        res.sort((p1, p2) -> p2.second.compareTo(p1.second));
        return res;
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


//    private List<Pair<String, Float>> resolveBySpellcorrectedEntityName(String name) {
//        List<Pair<String, Float>> results = new ArrayList<>();
//        try {
//            // TODO(dsavenk): These scores should probably be adjusted.
//            for (String suggest : spellChecker_.suggestSimilar(name, 50, 0.5f)) {
//                List<Pair<String, Float>> res = resolveByEntityName(suggest, 1, false, false);
//                results.addAll(res.stream().collect(Collectors.toList()));
//            }
//            if (results.size() > 0) {
//                results.sort((o1, o2) -> o2.second.compareTo(o1.second));
//                return results.stream().map(x -> new Pair<>(x.first, x.second / results.get(0).second)).collect(Collectors.toList());
//            }
//        } catch (IOException e) {}
//        return Collections.emptyList();
//    }
}
