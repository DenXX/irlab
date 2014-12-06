package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;

import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 12/5/14.
 */
public class EntityAddSoftDateRelationsProcessor extends Processor {

    private static final int MAX_IDS_COUNT = 5;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public EntityAddSoftDateRelationsProcessor(Properties properties) {
        super(properties);
        kb_ = KnowledgeBase.getInstance(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        Set<Triple<Integer, Integer, Integer>> existingRelations =
                document.getRelationList().stream().map(rel -> new Triple<>(
                        rel.getSubjectSpan(),
                        rel.getSubjectSpanCandidateEntityIdIndex(),
                        rel.getObjectSpan()))
                        .collect(Collectors.toSet());

        int objSpanIndex = 0;
        for (Document.Span objSpan : document.getSpanList()) {
            if (objSpan.getType().equals("MEASURE")
                    && objSpan.getNerType().equals("DATE")) {
                int subjSpanIndex = 0;
                for (Document.Span subjSpan : document.getSpanList()) {
                    if (subjSpan.hasEntityId()) {
                        for (int i = 0; i < Math.min(subjSpan.getCandidateEntityIdCount(), MAX_IDS_COUNT); ++i) {
                            if (!existingRelations.contains(new Triple<>(subjSpanIndex, i + 1, objSpanIndex))) {
                                List<KnowledgeBase.Triple> triples = kb_.getSubjectDateSoftMatchTriples(subjSpan.getCandidateEntityId(i), objSpan.getValue());
                                if (triples != null) {
                                    for (KnowledgeBase.Triple triple : triples) {
                                        Document.Relation.Builder relBuilder =
                                                docBuilder.addRelationBuilder();
                                        relBuilder.setObjectSpan(objSpanIndex);
                                        relBuilder.setSubjectSpan(subjSpanIndex);
                                        relBuilder.setRelation(triple.predicate);
                                        relBuilder.setSubjectSpanCandidateEntityIdIndex(i + 1);
                                    }
                                }
                            }
                        }
                    }
                   ++subjSpanIndex;
                }
            }
            ++objSpanIndex;
        }
        return docBuilder.build();
    }

    private final KnowledgeBase kb_;
}
