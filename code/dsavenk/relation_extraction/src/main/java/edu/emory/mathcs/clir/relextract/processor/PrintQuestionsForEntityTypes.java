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
                        String text = DocumentUtils.getSentenceTextWithEntityBoundary(document, mention, span.getEntityId());
                        for (String type : kb_.getEntityTypes(span.getCandidateEntityId(0))) {
                            System.out.println(type + "\t" + text);
                        }
                    }
                }
            }
        }
        return document;
    }
}
