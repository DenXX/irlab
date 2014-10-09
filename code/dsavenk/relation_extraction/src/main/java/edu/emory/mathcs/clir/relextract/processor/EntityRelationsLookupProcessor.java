package edu.emory.mathcs.clir.relextract.processor;

import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import edu.emory.mathcs.clir.relextract.AppParameters;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dsavenk on 10/8/14.
 */
public class EntityRelationsLookupProcessor extends Processor {
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
        logFilePath_ = properties.getProperty(AppParameters.LOGFILE_PARAMETER);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        boolean found = false;
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        int subjSpanIndex = -1;
        for (Document.Span subjSpan : document.getSpanList()) {
            ++subjSpanIndex;
            if (subjSpan.hasEntityId()) {
                final String subjMid = subjSpan.getEntityId();
                int objSpanIndex = -1;
                for (Document.Span objSpan : document.getSpanList()) {
                    ++objSpanIndex;
                    if (objSpan.hasEntityId()) {
                        String objMid = objSpan.getEntityId();
                        if (subjMid.equals(objMid)) continue;
//                        for (KnowledgeBase.Triple triple :
//                                kb_.getSubjectObjectTriplesCVT(subjMid,
//                                        objMid)) {
                        StmtIterator iter = kb_.getSubjectObjectTriples(subjMid, objMid);
                        while (iter.hasNext()) {
                            Statement tripleSt = iter.nextStatement();
                            KnowledgeBase.Triple triple =
                                    new KnowledgeBase.Triple(tripleSt);
                            Document.Relation.Builder relBuilder =
                                    Document.Relation.newBuilder();
                            relBuilder.setObjectSpan(objSpanIndex);
                            relBuilder.setSubjectSpan(subjSpanIndex);
                            relBuilder.setRelation(triple.predicate);
                            docBuilder.addRelation(relBuilder.build());
//                            predCounts_.putIfAbsent(triple.predicate, 0);
//                            predCounts_.put(triple.predicate,
//                                    predCounts_.get(triple.predicate) + 1);
//                            found = true;
                        }
                    } else {
                        // TODO(denxx): Implement measures lookup.
                    }
                }
            }
        }
//        if (found) {
//            docsWithRelation.incrementAndGet();
//        }
        return docBuilder.build();
    }

    @Override
    public void finishProcessing() {
//        try {
//            PrintWriter out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(logFilePath_)));
//            for (Map.Entry<String, Integer> predCount : predCounts_.entrySet()) {
//                out.println(predCount.getKey() + "\t" + predCount.getValue());
//            }
//            out.println(docsWithRelation.get());
//            out.close();
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
    }

    private final KnowledgeBase kb_;
//    private final ConcurrentMap<String, Integer> predCounts_ = new ConcurrentHashMap<>();
//    private final AtomicInteger docsWithRelation = new AtomicInteger(0);
    private final String logFilePath_;
}
