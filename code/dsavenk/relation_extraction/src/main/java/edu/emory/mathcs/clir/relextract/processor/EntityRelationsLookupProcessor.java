package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.util.Properties;
import java.util.Set;

/**
 * Created by dsavenk on 10/8/14.
 */
public class EntityRelationsLookupProcessor extends Processor {

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
                int subjEntityIdIndex = -1;
                for (String subjMid :
                        subjSpan.getCandidateEntityIdsList()) {
                    subjEntityIdIndex++;

                    // TODO(denxx): REMOVE THIS!
                    subjMid = subjSpan.getEntityId();
                    if (subjEntityIdIndex > 0) break;

                    int objSpanIndex = -1;
                    for (Document.Span objSpan : document.getSpanList()) {
                        ++objSpanIndex;
                        StmtIterator iter = null;
                        if (objSpan.hasEntityId()) {
                            int objEntityIdIndex = -1;
                            for (String objMid : objSpan.getCandidateEntityIdsList()) {
                                ++objEntityIdIndex;

                                // TODO(denxx): REMOVE THIS!
                                objMid = objSpan.getEntityId();
                                if (objEntityIdIndex > 0) break;

                                if (subjMid.equals(objMid)) continue;

                                Set<KnowledgeBase.Triple> triples = kb_.getSubjectObjectTriplesCVT(subjMid, objMid);
                                for (KnowledgeBase.Triple triple : triples) {
                                    Document.Relation.Builder relBuilder =
                                            Document.Relation.newBuilder();
                                    relBuilder.setObjectSpan(objSpanIndex);
                                    relBuilder.setSubjectSpan(subjSpanIndex);
                                    relBuilder.setRelation(triple.predicate);
                                    relBuilder.setSubjectSpanCandidateEntityIdIndex(subjEntityIdIndex);
                                    relBuilder.setObjectSpanCandidateEntityIdIndex(objEntityIdIndex);
                                    docBuilder.addRelation(relBuilder.build());
                                }

//                                iter = kb_.getSubjectObjectTriples(subjMid, objMid);
//                                // Now iterate over all triples and add them as annotations.
//                                while (iter != null && iter.hasNext()) {
//                                    Statement tripleSt = iter.nextStatement();
//                                    KnowledgeBase.Triple triple =
//                                            new KnowledgeBase.Triple(tripleSt);
//                                    Document.Relation.Builder relBuilder =
//                                            Document.Relation.newBuilder();
//                                    relBuilder.setObjectSpan(objSpanIndex);
//                                    relBuilder.setSubjectSpan(subjSpanIndex);
//                                    relBuilder.setRelation(triple.predicate);
//                                    relBuilder.setSubjectSpanCandidateEntityIdIndex(subjEntityIdIndex);
//                                    relBuilder.setObjectSpanCandidateEntityIdIndex(objEntityIdIndex);
//                                    docBuilder.addRelation(relBuilder.build());
//                                }
                            }
                        } else if (objSpan.getType().equals("MEASURE")) {
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
                                relBuilder.setSubjectSpanCandidateEntityIdIndex(
                                        subjEntityIdIndex);
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
