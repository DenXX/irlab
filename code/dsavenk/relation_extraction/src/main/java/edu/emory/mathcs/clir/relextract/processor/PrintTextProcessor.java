package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.Properties;

/**
 * Created by dsavenk on 9/26/14.
 */
public class PrintTextProcessor extends Processor {
    private int count = 0;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public PrintTextProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(
            Document.NlpDocument document) throws Exception {
        if (document.getRelationCount() > 0) {
            System.out.println(document.getText());
            for (Document.Relation rel : document.getRelationList()) {
                System.out.println(document.getSpan(rel.getSubjectSpan()).getValue() + " [" +
                        document.getSpan(rel.getSubjectSpan()).getEntityId() + "] " +
                        " -- " + rel.getRelation() + " -- " +
                        document.getSpan(rel.getObjectSpan()).getValue() + " [" +
                        document.getSpan(rel.getObjectSpan()).getEntityId() + "]");
            }
            System.out.println("---------------------------------------------");
            return document;
        }
        return null;
    }
}
