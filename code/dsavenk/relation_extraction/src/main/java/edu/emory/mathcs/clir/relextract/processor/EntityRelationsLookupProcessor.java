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
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        int subjSpanIndex = -1;
        for (Document.Span subjSpan : document.getSpanList()) {
            ++subjSpanIndex;
            if (subjSpan.hasEntityId()) {
                final String subjMid = subjSpan.getEntityId();
                int objSpanIndex = -1;
                for (Document.Span objSpan : document.getSpanList()) {
                    ++objSpanIndex;
                    StmtIterator iter = null;
                    if (objSpan.hasEntityId()) {
                        String objMid = objSpan.getEntityId();
                        if (subjMid.equals(objMid)) continue;
                        iter = kb_.getSubjectObjectTriples(subjMid, objMid);
                    } else if (objSpan.getType().equals("MEASURE")) {
                        // Now process measures.
                        iter = kb_.getSubjectMeasureTriples(subjMid,
                                objSpan.getValue(), objSpan.getNerType());
                    }

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
                        docBuilder.addRelation(relBuilder.build());
                    }
                }
            }
        }
        return docBuilder.build();
    }

}
