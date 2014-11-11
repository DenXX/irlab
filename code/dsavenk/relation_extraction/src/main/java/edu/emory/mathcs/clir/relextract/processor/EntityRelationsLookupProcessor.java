package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.util.Properties;

/**
 * Created by dsavenk on 10/8/14.
 */
public class EntityRelationsLookupProcessor extends Processor {

    private final int MAX_ENTITY_IDS_COUNT = 0;

    private final KnowledgeBase kb_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public EntityRelationsLookupProcessor(Properties properties) {
        super(properties);
        kb_ = KnowledgeBase.getInstance(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        docBuilder.clearRelation();
        int subjSpanIndex = -1;
        for (Document.Span subjSpan : document.getSpanList()) {
            ++subjSpanIndex;
            if (subjSpan.hasEntityId()) {
                int subjEntityIdIndex = 0;
                boolean seenSubjMainMid = false;
                for (String subjMid :
                        subjSpan.getCandidateEntityIdList()) {
                    subjEntityIdIndex++;
                    if (subjEntityIdIndex > MAX_ENTITY_IDS_COUNT) {
                        if (seenSubjMainMid) break;
                        else subjMid = subjSpan.getEntityId();
                    }

                    // Have we seen the "main" entity id.
                    if (subjMid.equals(subjSpan.getEntityId())) {
                        seenSubjMainMid = true;
                    }

                    int objSpanIndex = -1;
                    for (Document.Span objSpan : document.getSpanList()) {
                        ++objSpanIndex;
                        StmtIterator iter = null;
                        if (objSpan.hasEntityId()) {
                            int objEntityIdIndex = 0;
                            boolean seenObjMainMid = false;
                            for (String objMid : objSpan.getCandidateEntityIdList()) {
                                ++objEntityIdIndex;

                                if (objEntityIdIndex > MAX_ENTITY_IDS_COUNT) {
                                    if (seenObjMainMid) break;
                                    else objMid = objSpan.getEntityId();
                                }

                                if (objMid.equals(objSpan.getEntityId())) {
                                    seenObjMainMid = true;
                                }

                                if (subjMid.equals(objMid)) continue;

                                // Uncomment this code to search for length 2
                                // paths that go through a CVT.
//                                Set<KnowledgeBase.Triple> triples = kb_.getSubjectObjectTriplesCVT(subjMid, objMid);
//                                for (KnowledgeBase.Triple triple : triples) {
//                                    Document.Relation.Builder relBuilder =
//                                            Document.Relation.newBuilder();
//                                    relBuilder.setObjectSpan(objSpanIndex);
//                                    relBuilder.setSubjectSpan(subjSpanIndex);
//                                    relBuilder.setRelation(triple.predicate);
//                                    relBuilder.setSubjectSpanCandidateEntityIdIndex(subjEntityIdIndex);
//                                    relBuilder.setObjectSpanCandidateEntityIdIndex(objEntityIdIndex);
//                                    docBuilder.addRelation(relBuilder.build());
//                                }

                                iter = kb_.getSubjectObjectTriples(subjMid, objMid);
                                // Now iterate over all triples and add them as annotations.
                                while (iter != null && iter.hasNext()) {
                                    Statement tripleSt = iter.nextStatement();
                                    KnowledgeBase.Triple triple =
                                            new KnowledgeBase.Triple(tripleSt);
                                    Document.Relation.Builder relBuilder =
                                            Document.Relation.newBuilder();
                                    relBuilder.setObjectSpan(objSpanIndex);
                                    relBuilder.setSubjectSpan(subjSpanIndex);
                                    relBuilder.setRelation(triple.predicate);

                                    // If current id is the "main" id, we won't set
                                    // the index so we can later figure this out.
                                    if (!subjMid.equals(subjSpan.getEntityId())) {
                                        relBuilder.setSubjectSpanCandidateEntityIdIndex(subjEntityIdIndex);
                                    }
                                    if (objMid.equals(objSpan.getEntityId())) {
                                        relBuilder.setObjectSpanCandidateEntityIdIndex(objEntityIdIndex);
                                    }
                                    docBuilder.addRelation(relBuilder.build());
                                }
                            }
                        } else if (objSpan.getType().equals("MEASURE") &&
                                (objSpan.getNerType().equals("DATE") ||
                                        objSpan.getNerType().equals("TIME"))) {
                            // Now process measures.
                            iter = kb_.getSubjectMeasureTriples(subjMid,
                                    objSpan.getValue(), objSpan.getNerType());
                            // TODO(denxx): Do not duplicate this piece of code.
                            // Now iterate over all triples and add them as annotations.
                            while (iter != null && iter.hasNext()) {
                                Statement tripleSt = iter.nextStatement();
                                KnowledgeBase.Triple triple =
                                        new KnowledgeBase.Triple(tripleSt);
                                Document.Relation.Builder relBuilder =
                                        Document.Relation.newBuilder();
                                relBuilder.setObjectSpan(objSpanIndex);
                                relBuilder.setSubjectSpan(subjSpanIndex);
                                relBuilder.setRelation(triple.predicate);
                                if (!subjMid.equals(subjSpan.getEntityId())) {
                                    relBuilder.setSubjectSpanCandidateEntityIdIndex(
                                            subjEntityIdIndex);
                                }
                                docBuilder.addRelation(relBuilder.build());
                            }
                        }
                    }
                }
            }
        }
        return docBuilder.build();
    }

}
