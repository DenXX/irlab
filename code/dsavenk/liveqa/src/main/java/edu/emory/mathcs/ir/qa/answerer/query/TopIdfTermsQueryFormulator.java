package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import edu.emory.mathcs.ir.utils.ObjectWithScore;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Query formulator that generates a query using top terms from the question
 * based on top question terms by idf.
 */
public class TopIdfTermsQueryFormulator implements QueryFormulation {
    private final IndexSearcher searcher_;
    private boolean includeBody_;
    private double idfMaxRatioThreshold_;

    public TopIdfTermsQueryFormulator(IndexReader reader,
                                      boolean includeBody,
                                      double idfMaxRatioThreshold) {
        searcher_ = new IndexSearcher(reader);
        includeBody_ = includeBody;
        idfMaxRatioThreshold_ = idfMaxRatioThreshold;
    }

    @Override
    public String getQuery(Question question) {
        Text text = question.getTitle();
        if (includeBody_) {
            text = text.concat(question.getBody());
        }
        return String.join(" ", getTopTermsByIdfRatio(text));
    }

    private String[] getTopTermsByIdfRatio(Text text) {
        List<ObjectWithScore<String>> termsScores =
                Arrays.stream(text.getTokens())
                .map(token -> QnAIndexDocument.getAnalyzedTerm(token.text))
                .filter(term -> !term.isEmpty())
                .map(term -> new ObjectWithScore<>(term, idf(term)))
                .sorted(Collections.reverseOrder())
                .collect(Collectors.toList());

        if (!termsScores.isEmpty()) {
            final double maxScore = termsScores.get(0).score;
            return termsScores.stream().filter(termIdf ->
                    termIdf.score / maxScore < idfMaxRatioThreshold_)
                    .map(termIdf -> termIdf.object)
                    .toArray(String[]::new);
        }
        return new String[0];
    }

    private double idf(String term) {
        int termFreq = getTermDocFreq(term);
        return Math.log(1 + (
                searcher_.getIndexReader().numDocs() - termFreq + 0.5D) /
                (termFreq + 0.5D));
    }

    private int getTermDocFreq(String termText) {
        try {
            final Term t =
                    new Term(QnAIndexDocument.QTITLEBODY_FIELD_NAME,
                            termText);
            return searcher_.getIndexReader().docFreq(t);
        } catch (IOException e) {
            return 0;
        }
    }
}
