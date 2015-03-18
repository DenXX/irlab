package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.DocumentUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.io.*;
import java.util.*;

/**
 * Created by dsavenk on 2/16/15.
 */
public class PrintQuestionsForEntityTypes extends Processor {

    public static final String ENTITY_TYPES_PARAMETER = "entity_types_file";

    private KnowledgeBase kb_ = null;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public PrintQuestionsForEntityTypes(Properties properties) throws IOException {
        super(properties);
        kb_ = KnowledgeBase.getInstance(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        int questionSentences = DocumentUtils.getQuestionSentenceCount(document);
        for (Document.Span span : document.getSpanList()) {
            if (!span.getNerType().equals("NONE") && span.hasEntityId() && span.getCandidateEntityScore(0) >= Parameters.MIN_ENTITYID_SCORE) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (!mention.getType().equals("OTHER") && mention.getSentenceIndex() < questionSentences) {
                        for (int i = 0; i < Math.min(1, span.getCandidateEntityIdCount()); ++i) {
                            if (span.getCandidateEntityScore(i) < Parameters.MIN_ENTITYID_SCORE) break;
                            String text = DocumentUtils.getSentenceTextWithEntityBoundary(document, mention, span.getCandidateEntityId(i)).replace("\n", " ").replace("\t", " ");

                            StringBuilder qwords = new StringBuilder();
                            StringBuilder verbs = new StringBuilder();
                            StringBuilder nouns = new StringBuilder();
                            for (int j = document.getSentence(mention.getSentenceIndex()).getFirstToken();
                                 j < document.getSentence(mention.getSentenceIndex()).getLastToken(); ++j) {
                                if (j < mention.getTokenBeginOffset() || j >= mention.getTokenEndOffset()) {
                                    if (document.getToken(j).getPos().startsWith("W")) {
                                        qwords.append(document.getToken(j).getLemma() + ",");
                                    } else if (document.getToken(j).getPos().startsWith("V")) {
                                        verbs.append(document.getToken(j).getLemma() + ",");
                                    } else if (document.getToken(j).getPos().startsWith("N")) {
                                        nouns.append(document.getToken(j).getLemma() + ",");
                                    }
                                }
                            }
                            if (qwords.length() == 0) {
                                qwords.append(document.getToken(document.getSentence(mention.getSentenceIndex()).getFirstToken()).getLemma() + ",");
                            }

                            for (String type : kb_.getEntityTypes(span.getCandidateEntityId(0))) {
                                System.out.println(type + "\t" + text + "\t" + qwords.toString() + "\t" + verbs.toString() + "\t" + nouns.toString());
                            }
                        }
                    }
                }
            }
        }
        return document;
    }
}
