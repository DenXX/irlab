package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.Properties;

/**
 * Created by dsavenk on 10/17/14.
 */
public class QuestionAnswerExtractorTrainingDataProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public QuestionAnswerExtractorTrainingDataProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (document.getRelationCount() == 0) {
            return null;
        }

        rel:
        for (Document.Relation rel : document.getRelationList()) {
            if (rel.getRelation().equals("book.written_work.author")) {
                for (Document.Mention subjMention :
                        document.getSpan(rel.getSubjectSpan()).getMentionList()) {
                    String subjId = document.getSpan(rel.getSubjectSpan()).getEntityId();
                    boolean subjInQuestion =
                            document.getToken(subjMention.getTokenBeginOffset()).getBeginCharOffset() < document.getQuestionLength();
                    for (Document.Mention objMention :
                            document.getSpan(rel.getObjectSpan()).getMentionList()) {
                        boolean objInQuestion =
                                document.getToken(objMention.getTokenBeginOffset()).getBeginCharOffset() < document.getQuestionLength();
                        if (subjInQuestion != objInQuestion) {
                            String objId = document.getSpan(rel.getObjectSpan()).hasEntityId() ? document.getSpan(rel.getObjectSpan()).getEntityId() : document.getSpan(rel.getObjectSpan()).getValue();
                            System.out.println("Subject: \n" + document.getSentence(subjMention.getSentenceIndex()).getText().replace("\n", " "));
                            System.out.println("Object: \n" + document.getSentence(objMention.getSentenceIndex()).getText().replace("\n", " "));
                            System.out.println(subjMention.getText() + " [" + subjId + "] - " + rel.getRelation() + " - " + objMention.getText() + " [" + objId + "] ");
                            System.out.println();
                            continue rel;
                        }
                    }
                }
            }
        }
        return document;
    }
}
