package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import edu.emory.mathcs.ir.utils.NlpUtils;
import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.util.Span;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
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
    private Analyzer analyzer_ = new EnglishAnalyzer(
            CharArraySet.copy(NlpUtils.getStopwords()));

    public TestQueryFormulator(IndexReader reader) {
        try {
            chunker_ = new ChunkerME(new ChunkerModel(
                    TestQueryFormulator.class.getResourceAsStream(
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
        weightIdf(question.getTitle());
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
                if (token.isWord()) {
                    String term = QnAIndexDocument.getAnalyzedTerm(token.text);
                    if (!term.isEmpty()) {
                        System.err.println(term + "\t" + idf(term));
                    }
                }
            }
        }
    }

    private double idf(String term) {
        int termFreq = getTermDocFreq(term);
        return Math.log(1 + (
                indexSearcher_.getIndexReader().numDocs() - termFreq + 0.5D) /
                (termFreq + 0.5D));
    }

    private int getTermDocFreq(String termText) {
        try {
            final Term t =
                    new Term(QnAIndexDocument.QTITLEBODY_FIELD_NAME,
                            termText);
            return indexSearcher_.getIndexReader().docFreq(t);
        } catch (IOException e) {
            return 0;
        }
    }
}
