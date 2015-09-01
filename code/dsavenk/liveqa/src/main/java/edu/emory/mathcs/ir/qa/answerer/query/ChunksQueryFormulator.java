package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.Span;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 8/31/15.
 */
public class ChunksQueryFormulator implements QueryFormulation {
    private Chunker chunker_;

    public ChunksQueryFormulator() {
        try {
            chunker_ = new ChunkerME(new ChunkerModel(
                    TestQueryFormulator.class.getResourceAsStream(
                            "/en-chunker.bin")));
        } catch (IOException e) {
            LiveQaLogger.LOGGER.warning(e.getMessage());
            chunker_ = null;
        }
    }

    @Override
    public String getQuery(Question question) {
        List<String> allChunks = new ArrayList<>();
        Map<String, Integer> chunksTf = new HashMap<>();
        for (Text.Sentence sent : question.getTitle().getSentences()) {
            String[] tokens = Arrays.stream(sent.tokens)
                    .map(token -> token.text)
                    .toArray(String[]::new);
            String[] pos = Arrays.stream(sent.tokens)
                    .map(token -> token.pos)
                    .toArray(String[]::new);
            Span[] chunks = chunker_.chunkAsSpans(tokens, pos);
            for (Span chunk : chunks) {
                String phrase = Arrays.stream(Arrays.copyOfRange(
                        sent.tokens, chunk.getStart(), chunk.getEnd()))
                        .map(token -> token.text)
                        .collect(Collectors.joining(" "));
                allChunks.add(phrase);
            }
        }
        return String.join(" ", allChunks);
    }
}
