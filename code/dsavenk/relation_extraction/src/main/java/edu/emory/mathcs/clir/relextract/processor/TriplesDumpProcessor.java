package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;

import java.util.Properties;

/**
 * Created by dsavenk on 11/19/14.
 */
public class TriplesDumpProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public TriplesDumpProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (document.getRelationCount() == 0) return null;

        int questionTokens = 0;
        while (questionTokens < document.getTokenCount()
                && document.getToken(questionTokens).getBeginCharOffset() < document.getQuestionLength()) {
            ++questionTokens;
        }

        for (Document.Relation rel : document.getRelationList()) {
            int subjIdIndex = rel.getSubjectSpanCandidateEntityIdIndex();
            int objIdIndex = rel.getObjectSpanCandidateEntityIdIndex();

            if (document.getSpan(rel.getSubjectSpan()).getCandidateEntityScore(subjIdIndex - 1) >= Parameters.MIN_ENTITYID_SCORE &&
                    (!document.getSpan(rel.getObjectSpan()).hasEntityId()
                            || document.getSpan(rel.getObjectSpan()).getCandidateEntityScore(objIdIndex - 1) >= Parameters.MIN_ENTITYID_SCORE)) {

                String subject = document.getSpan(rel.getSubjectSpan()).getCandidateEntityId(subjIdIndex - 1);
                String object = document.getSpan(rel.getObjectSpan()).hasEntityId()
                        ? document.getSpan(rel.getObjectSpan()).getCandidateEntityId(objIdIndex - 1)
                        : document.getSpan(rel.getObjectSpan()).getValue().replaceAll("\n", " ").replace("\t", " ");
                for (Document.Mention subjMention : document.getSpan(rel.getSubjectSpan()).getMentionList()) {
                    for (Document.Mention objMention : document.getSpan(rel.getObjectSpan()).getMentionList()) {
                        boolean subjectInQuestion = subjMention.getTokenBeginOffset() < questionTokens;
                        boolean objectInQuestion = objMention.getTokenBeginOffset() < questionTokens;
                        if (subjectInQuestion && objectInQuestion &&
                                subjMention.getSentenceIndex() == objMention.getSentenceIndex()) {
                            System.out.println(subject + "\t" + rel.getRelation() + "\t" + object + "\t" + document.getDocId() + "\tQ");
                        } else if (subjectInQuestion != objectInQuestion) {
                            System.out.println(subject + "\t" + rel.getRelation() + "\t" + object + "\t" + document.getDocId() + "\tQA");
                        } else if (!subjectInQuestion && !objectInQuestion && subjMention.getSentenceIndex() == objMention.getSentenceIndex()) {
                            System.out.println(subject + "\t" + rel.getRelation() + "\t" + object + "\t" + document.getDocId() + "\tA");
                        }
                    }
                }
            }
        }
        return document;
    }
}
