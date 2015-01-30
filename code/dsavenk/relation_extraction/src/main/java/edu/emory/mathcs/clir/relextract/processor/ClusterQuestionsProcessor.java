package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.utils.DocumentUtils;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Created by dsavenk on 1/29/15.
 */
public class ClusterQuestionsProcessor extends Processor {

    private KnowledgeBase kb_;
    private Map<String, List<String>> entityTypes_ = new HashMap<>();

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public ClusterQuestionsProcessor(Properties properties) {
        super(properties);
        kb_ = KnowledgeBase.getInstance(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        int questionSentencesCount = DocumentUtils.getQuestionSentenceCount(document);

        for (Document.Span span : document.getSpanList()) {
            boolean isResolvedEntity = span.hasEntityId();
            if (isResolvedEntity) {
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < questionSentencesCount) {
                        for (String mid : span.getCandidateEntityIdList().subList(0, Math.min(5, span.getCandidateEntityIdCount()))) {
                            if (!entityTypes_.containsKey(mid)) entityTypes_.put(mid, kb_.getEntityTypes(mid));
                            for (String type : entityTypes_.get(mid)) {
                                System.out.println(type + "\t" + document.getSentence(mention.getSentenceIndex()).getText().replace("\t", " ").replace("\n", " "));
                            }
                        }
                    }
                }
            }
        }

        return null;
    }
}
