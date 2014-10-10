package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.AppParameters;
import edu.emory.mathcs.clir.relextract.data.Document;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by dsavenk on 10/9/14.
 */
public class RelationsStatsProcessor extends Processor {
    private final AtomicInteger docsWithRelation_ = new AtomicInteger();
    private final AtomicInteger totalDocs_ = new AtomicInteger();
    private String logFilename_;
    private ConcurrentMap<String, Integer> relationCount_ = new ConcurrentHashMap<>();
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public RelationsStatsProcessor(Properties properties) {
        super(properties);
        logFilename_ = properties.getProperty(AppParameters.LOGFILE_PARAMETER);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        totalDocs_.incrementAndGet();
        if (document.getRelationCount() == 0) {
            return null;
        }
        docsWithRelation_.incrementAndGet();
        System.out.println(document.getText());
        for (Document.Relation relation : document.getRelationList()) {
            System.out.println(
                    document.getSpan(relation.getSubjectSpan()).getText() +
                            " [" + document.getSpan(relation.getSubjectSpan()).getEntityId() +
                            " ] -- " + relation.getRelation() + " -- " +
                            document.getSpan(relation.getObjectSpan()).getText() +
                            " [" + document.getSpan(relation.getObjectSpan()).getEntityId() +
                            " ]");
            relationCount_.putIfAbsent(relation.getRelation(), 0);
            relationCount_.put(relation.getRelation(),
                    relationCount_.get(relation.getRelation()) + 1);
        }
        System.out.println("------------------------------------");
        return document;
    }

    @Override
    public void finishProcessing() {
        synchronized (this) {
            try {
                PrintWriter out = new PrintWriter(new BufferedOutputStream(
                        new FileOutputStream(logFilename_)));
                for (String relation : relationCount_.keySet()) {
                    out.println(relation + "\t" + relationCount_.get(relation));
                }
                out.println("--------------------");
                out.println("Documents with relations: " + docsWithRelation_);
                out.println("Total Documents: " + totalDocs_);
                out.close();
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}
