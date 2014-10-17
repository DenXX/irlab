package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.Properties;

/**
 * Created by dsavenk on 10/17/14.
 */
public class SentenceExtractorTrainingDataProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public SentenceExtractorTrainingDataProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (document.getRelationCount() == 0) {
            return null;
        }

        for (Document.Relation rel : document.getRelationList()) {
            if (rel.getRelation().equals("people.person.place_of_birth")) {
                for (Document.Mention subjMention :
                        document.getSpan(rel.getSubjectSpan()).getMentionList()) {
                    String subjId = document.getSpan(rel.getSubjectSpan()).getEntityId();
                    for (Document.Mention objMention :
                            document.getSpan(rel.getObjectSpan()).getMentionList()) {
                        if (subjMention.getSentenceIndex() == objMention.getSentenceIndex()) {
                            String objId = document.getSpan(rel.getObjectSpan()).hasEntityId() ? document.getSpan(rel.getObjectSpan()).getEntityId() : document.getSpan(rel.getObjectSpan()).getValue();
                            System.out.println(document.getSentence(subjMention.getSentenceIndex()).getText().replace("\n", " "));
                            System.out.println(subjMention.getText() + " [" + subjId + "] - " + rel.getRelation() + " - " + objMention.getText() + " [" + objId + "] ");
                        }
                    }
                }
            }
        }
        return document;
    }
}
