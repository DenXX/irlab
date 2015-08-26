package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.ml.BM25FeatureGenerator;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * Created by dsavenk on 8/26/15.
 */
public class BM25AnswerScorer implements AnswerScoring {
    private final BM25FeatureGenerator featureGenerator_;

    public BM25AnswerScorer(IndexReader reader) throws IOException {
        featureGenerator_ = new BM25FeatureGenerator(reader);
    }

    @Override
    public double scoreAnswer(Question question, Answer answer) {
        return featureGenerator_.generateFeatures(question, answer)
                .getOrDefault(BM25FeatureGenerator.
                        TITLEBODY_ANSWER_BM25_FEATURENAME, 0.0);
    }
}
