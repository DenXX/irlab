package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.utils.NlpUtils;
import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.Span;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 8/26/15.
 */
public class TestQueryFormulator implements QueryFormulation {
    private Chunker chunker_;
    private IndexSearcher indexSearcher_;

    public TestQueryFormulator(IndexReader reader) {
        try {
            chunker_ = new ChunkerME(new ChunkerModel(
                    TitleNoStopwordsQueryFormulator.class.getResourceAsStream(
                            "/en-chunker.bin")));
        } catch (IOException e) {
            LiveQaLogger.LOGGER.warning(e.getMessage());
            chunker_ = null;
        }
        indexSearcher_ = new IndexSearcher(reader);

    }

    @Override
    public String getQuery(Question question) {
        String query = removeStopwords(question.getTitle());
        if (query.isEmpty()) {
            query = removeStopwords(question.getBody());
        }
        return query;
    }

    private String removeStopwords(Text text) {
        return Arrays.stream(text.getTokens())
                .filter(token -> !NlpUtils.getStopwords().contains(token.lemma)
                        && Character.isAlphabetic(token.pos.charAt(0)))
                .map(token -> token.lemma)
                .collect(Collectors.joining(" ")).trim();
    }

    private void chunkerTest(Text text) {
        for (Text.Sentence sent : text.getSentences()) {
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
                System.out.println(chunk.getType() + "\t" + phrase);
            }
        }
    }

    private void weightIdf(Text text) {
        for (Text.Sentence sent : text.getSentences()) {
            for (Text.Token token : sent.tokens) {

            }
        }
    }
}
