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
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Query formulator that generates a query using top terms from the question
 * based on top question terms by idf.
 */
public class TopIdfTermsQueryFormulator implements QueryFormulation {
    private final IndexSearcher searcher_;
    private boolean includeBody_;
    private int topN_;

    public TopIdfTermsQueryFormulator(IndexReader reader,
                                      boolean includeBody,
                                      int topN) {
        searcher_ = new IndexSearcher(reader);
        includeBody_ = includeBody;
        topN_ = topN;
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
        Map<String, Double> termsIdfScores =
                Arrays.stream(text.getTokens())
                        .filter(term ->
                                !QnAIndexDocument.getAnalyzedTerm(
                                        term.text).isEmpty())
                        .collect(Collectors.toMap(term -> term.lemma,
                                term -> idf(QnAIndexDocument.getAnalyzedTerm(
                                        term.text)), (e1, e2) -> e1));

        Map<String, Long> termsTfScores =
                Arrays.stream(text.getTokens())
                        .filter(term -> !QnAIndexDocument.getAnalyzedTerm(
                                term.text).isEmpty())
                        .collect(Collectors.groupingBy(term -> term.lemma,
                                Collectors.counting()));

        if (!termsIdfScores.isEmpty()) {
            final long maxTf = termsTfScores.entrySet()
                    .stream()
                    .max((e1, e2) -> e1.getValue().compareTo(e2.getValue()))
                    .get().getValue();

            List<String> termScores =
                    termsIdfScores.entrySet()
                            .stream()
                            .map(e -> new ObjectWithScore<>(e.getKey(),
                                    e.getValue() * (0.5 + 0.5 *
                                            termsTfScores.get(e.getKey()) / maxTf)))
                            .sorted(Comparator.reverseOrder())
                            .map(term -> term.object)
                            .collect(Collectors.toList());

            return termScores.subList(0, Math.min(termScores.size(), topN_))
                    .toArray(new String[Math.min(termScores.size(), topN_)]);
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

    @Override
    public String toString() {
        return "tf-idf";
    }
}
