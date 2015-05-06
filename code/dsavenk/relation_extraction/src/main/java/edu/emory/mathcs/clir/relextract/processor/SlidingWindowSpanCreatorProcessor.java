package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.util.Pair;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

/**
 * Created by dsavenk on 5/5/15.
 */
public class SlidingWindowSpanCreatorProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public SlidingWindowSpanCreatorProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();

        Set<Pair<Integer, Integer>> existingSpans = new HashSet<>();
        for (Document.Span span : document.getSpanList()) {
            for (Document.Mention mention : span.getMentionList()) {
                existingSpans.add(new Pair<>(mention.getTokenBeginOffset(), mention.getTokenEndOffset()));
            }
        }

        for (int startTokenIndex = 0; startTokenIndex < document.getTokenCount(); ++startTokenIndex) {
            Document.Token startToken = document.getToken(startTokenIndex);
            if (!Character.isAlphabetic(startToken.getPos().charAt(0))) continue;
            boolean flag = false;
            for (int endTokenIndex = startTokenIndex; endTokenIndex < document.getTokenCount(); ++endTokenIndex) {
                Document.Token endToken = document.getToken(endTokenIndex);
                if (endToken.getSentenceIndex() != startToken.getSentenceIndex()) break;

                if (!Character.isAlphabetic(endToken.getPos().charAt(0))) continue;

                if (existingSpans.contains(new Pair<>(startTokenIndex, endTokenIndex + 1))) continue;

                String text = document.getText().substring(startToken.getBeginCharOffset(), endToken.getEndCharOffset());
                if (!text.startsWith("<") || text.endsWith(">")) {
                    if (text.startsWith("<") && text.endsWith(">")) {
                        text = text.substring(1, text.length() - 1);
                        flag = true;
                    }
                    docBuilder.addSpanBuilder()
                            .setText(text)
                            .setValue(text)
                            .setType("ENTITY")
                            .setNerType("NONE")
                            .setRepresentativeMention(0)
                            .addMentionBuilder()
                            .setMentionType("NOMINAL")
                            .setSentenceIndex(startToken.getSentenceIndex())
                            .setText(text)
                            .setType("ENTITY")
                            .setTokenBeginOffset(startTokenIndex)
                            .setTokenEndOffset(endTokenIndex)
                            .setValue(text);
                }

                if (flag) {
                    startTokenIndex = endTokenIndex;
                    break;
                }
            }
        }
        return docBuilder.build();
    }
}
