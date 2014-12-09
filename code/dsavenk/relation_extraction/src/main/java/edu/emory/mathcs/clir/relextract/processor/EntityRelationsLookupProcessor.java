package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import edu.emory.mathcs.clir.relextract.data.Dataset;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.util.Pair;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by dsavenk on 10/8/14.
 */
public class EntityRelationsLookupProcessor extends Processor {

    public static final String CVT_PREDICATES_LIST_PARAMETER = "cvt";
    public static final String SOFT_DATE_PARAMETER = "soft_date";

    private final int MAX_ENTITY_IDS_COUNT = 5;

    private final KnowledgeBase kb_;

    private boolean softDateMatch_ = false;
    private final List<Pair<String, String>> cvtProperties_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public EntityRelationsLookupProcessor(Properties properties) throws IOException {
        super(properties);
        kb_ = KnowledgeBase.getInstance(properties);
        if (properties.containsKey(CVT_PREDICATES_LIST_PARAMETER)) {
            cvtProperties_ = new ArrayList<>();
            BufferedReader input = new BufferedReader(new FileReader(properties.getProperty(CVT_PREDICATES_LIST_PARAMETER)));
            String line;
            while ((line = input.readLine()) != null) {
                String[] parts = line.split("\\.");
                cvtProperties_.add(new Pair<>(parts[0].substring(1).replaceAll("/", "."),
                        parts[1].substring(1).replaceAll("/", ".")));
            }
        } else {
            cvtProperties_ = null;
        }

        if (properties.containsKey(SOFT_DATE_PARAMETER)) {
            softDateMatch_ = Boolean.parseBoolean(properties.getProperty(SOFT_DATE_PARAMETER));
        }
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
                for (String subjMid :
                        subjSpan.getCandidateEntityIdList()) {
                    subjEntityIdIndex++;
                    if (subjEntityIdIndex > MAX_ENTITY_IDS_COUNT) {
                        break;
                    }

                    int objSpanIndex = -1;
                    for (Document.Span objSpan : document.getSpanList()) {
                        ++objSpanIndex;

                        if (objSpanIndex == subjSpanIndex) continue;

                        StmtIterator iter = null;
                        if (objSpan.hasEntityId()) {
                            int objEntityIdIndex = 0;
                            for (String objMid : objSpan.getCandidateEntityIdList()) {
                                ++objEntityIdIndex;
                                if (objEntityIdIndex > MAX_ENTITY_IDS_COUNT) {
                                    break;
                                }
                                if (subjMid.equals(objMid)) continue;

                                // Uncomment this code to search for length 2
                                // paths that go through all possible CVTs.
//                                Set<KnowledgeBase.Triple> triples = kb_.getSubjectObjectTriplesCVT(subjMid, objMid);
//                                for (KnowledgeBase.Triple triple : triples) {
//                                    Document.Relation.Builder relBuilder =
//                                            Document.Relation.newBuilder();
//                                    relBuilder.setObjectSpan(objSpanIndex);
//                                    relBuilder.setSubjectSpan(subjSpanIndex);
//                                    relBuilder.setRelation(triple.predicate);
//                                    if (!subjMid.equals(subjSpan.getEntityId())) {
//                                        relBuilder.setSubjectSpanCandidateEntityIdIndex(subjEntityIdIndex);
//                                    }
//                                    if (!objMid.equals(objSpan.getEntityId())) {
//                                        relBuilder.setObjectSpanCandidateEntityIdIndex(objEntityIdIndex);
//                                    }
//                                    docBuilder.addRelation(relBuilder.build());
//                                }

                                List<KnowledgeBase.Triple> triples = kb_.getSubjectObjectTriples(subjMid, objMid, cvtProperties_);
                                if (triples != null) {
                                    // Now iterate over all triples and add them as annotations.
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
                                }
                            }
                        } else if (objSpan.getType().equals("MEASURE") &&
                                (objSpan.getNerType().equals("DATE") ||
                                        objSpan.getNerType().equals("TIME"))) {

                            List<KnowledgeBase.Triple> triples = objSpan.getNerType().equals("DATE") && softDateMatch_
                                ? kb_.getSubjectDateSoftMatchTriples(subjMid, objSpan.getValue())
                                : kb_.getSubjectMeasureTriples(subjMid, objSpan.getValue(), objSpan.getNerType());

                            // Now add all these triples.
                            if (triples != null) {
                                for (KnowledgeBase.Triple triple : triples) {
                                    Document.Relation.Builder relBuilder =
                                            Document.Relation.newBuilder();
                                    relBuilder.setObjectSpan(objSpanIndex);
                                    relBuilder.setSubjectSpan(subjSpanIndex);
                                    relBuilder.setRelation(triple.predicate);
                                    relBuilder.setSubjectSpanCandidateEntityIdIndex(subjEntityIdIndex);
                                    docBuilder.addRelation(relBuilder.build());
                                }
                            }
                        }
                    }
                }
            }
        }
        return docBuilder.build();
    }
}
