package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by dsavenk on 10/14/14.
 */
public class FixCorefAnnotationProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public FixCorefAnnotationProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws Exception {
        Map<Pair<Integer, Integer>, Integer> spanTokens = new HashMap<>();
        int spanIndex = 0;
        for (Document.Span span : document.getSpanList()) {
            spanTokens.put(new Pair<>(span.getTokenBeginOffset(),
                    span.getTokenEndOffset()), spanIndex++);
        }

        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        int index = 0;
        for (Document.CorefCluster.Builder coref :
                docBuilder.getCorefClusterBuilderList()) {
            for (Document.Span.Builder mention :
                    coref.getMentionBuilderList()) {
                int sentFirstToken = document.getSentence(
                        mention.getSentenceIndex() - 1).getFirstToken();
                mention.setSentenceIndex(mention.getSentenceIndex() - 1);
                int firstToken = mention.getTokenBeginOffset() +
                        sentFirstToken - 1;
                int endToken = mention.getTokenEndOffset() + sentFirstToken - 1;
                mention.setTokenBeginOffset(firstToken);
                mention.setTokenEndOffset(endToken);
                for (int tokenIndex = firstToken; tokenIndex < endToken;
                     ++tokenIndex) {
                    docBuilder.getTokenBuilder(tokenIndex).setCorefClusterId(
                            index);
                }

                // If this mention corresponds to a entity/measure span,
                // fill the corresponding fields in the span.
                if (spanTokens.containsKey(
                        new Pair<>(firstToken, endToken))) {
                    Document.Span.Builder spanBuilder =
                            docBuilder.getSpanBuilder(
                                    spanTokens.get(
                                            new Pair<>(firstToken,
                                                    endToken)));
                    spanBuilder.setCorefClusterIndex(index)
                            .setGender(mention.getGender())
                            .setAnimacy(mention.getAnimacy())
                            .setMentionType(mention.getMentionType());
                }
            }
            ++index;
        }
        return docBuilder.build();
    }
}
