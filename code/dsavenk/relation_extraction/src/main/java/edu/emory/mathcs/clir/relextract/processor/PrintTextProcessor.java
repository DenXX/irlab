package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.util.Pair;
import edu.stanford.nlp.util.Triple;
import org.omg.PortableInterceptor.SYSTEM_EXCEPTION;

import java.util.*;

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

        Map<Integer, List<Pair<Integer, Integer>>> beginToken2Mention = new HashMap<>();
        for (int spanIndex = 0; spanIndex < document.getSpanCount(); ++spanIndex) {
            Document.Span span = document.getSpan(spanIndex);
            for (int mentionIndex = 0; mentionIndex < span.getMentionCount(); ++mentionIndex) {
                Document.Mention mention = span.getMention(mentionIndex);
                int firstToken = mention.getTokenBeginOffset();
                if (!beginToken2Mention.containsKey(firstToken)) {
                    beginToken2Mention.put(firstToken, new ArrayList<Pair<Integer, Integer>>());
                }
                beginToken2Mention.get(firstToken).add(new Pair<>(spanIndex, mentionIndex));
            }
        }

        PriorityQueue<Triple<Integer, Integer, Integer>> currentMentions = new PriorityQueue<>();
        int prevSentenceIndex = 0;
        for (int tokenIndex = 0;
             tokenIndex < document.getTokenCount(); ++tokenIndex) {
            if (document.getToken(tokenIndex).getSentenceIndex() != prevSentenceIndex) {
                System.out.println();
                prevSentenceIndex = document.getToken(tokenIndex).getSentenceIndex();
            }
            if (beginToken2Mention.containsKey(tokenIndex)) {
                for (Pair<Integer, Integer> mention : beginToken2Mention.get(tokenIndex)) {
                    Document.Span span = document.getSpan(mention.first);
                    Document.Mention spanMention = span.getMention(mention.second);
                    currentMentions.add(new Triple<>(spanMention.getTokenEndOffset(), mention.first, mention.second));
                    String spanTypeStr = (span.hasEntityId()
                            ? ":" + span.getEntityId()
                            : (span.getType().equals("MEASURE") ? ":" + span.getValue() : ""));
                    String mentionTypeStr = (spanMention.hasEntityId()
                            ? ":" + spanMention.getEntityId()
                            : (spanMention.getType().equals("MEASURE") ? ":" + spanMention.getValue() : ""));
                    String showReprMention = span.getRepresentativeMention() == mention.second
                            ? "!"
                            : "";
                    System.out.print("<" + showReprMention  + mention.first + span.getType().charAt(0) + spanTypeStr + "|" +
                            spanMention.getType().charAt(0) + mentionTypeStr + " - ");
                }
            }
            System.out.print(document.getToken(tokenIndex).getText() + " ");
            printMentionEnds(document, currentMentions, tokenIndex);
        }
        printMentionEnds(document, currentMentions, document.getTokenCount());

        System.out.println("\n------------------------------------------------------------------------");
        return null;
    }

    private void printMentionEnds(Document.NlpDocument document, PriorityQueue<Triple<Integer, Integer, Integer>> currentMentions, int tokenIndex) {
        while (currentMentions.size() != 0) {
            Triple<Integer, Integer, Integer> nextMention = currentMentions.peek();
            if (nextMention.first - 1 <= tokenIndex) {
                System.out.print(nextMention.second + "> ");
                currentMentions.poll();
            } else {
                break;
            }
        }
    }

    @Override
    public void finishProcessing() {
        System.out.println(count);
    }
}
