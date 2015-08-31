package edu.emory.mathcs.ir.qa.answerer.yahooanswers;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.qa.answerer.QuestionAnswering;
import edu.emory.mathcs.ir.qa.answerer.query.*;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerSelection;
import edu.emory.mathcs.ir.qa.answerer.ranking.FeatureBasedAnswerSelector;
import edu.emory.mathcs.ir.qa.ml.*;
import org.apache.lucene.index.IndexReader;

import java.io.IOException;

/**
 * QA based on the best answer to the related questions from Yahoo! Answers.
 * Related questions are obtained using Yahoo! Answers search.
 */
public class YahooAnswersBasedQuestionAnswerer implements QuestionAnswering {
    /**
     * The number of top similar questions to extract answers from.
     */
    public static final int TOPN_SIMILAR_QUESTIONS = 10;

    /**
     * Default answer returned in case we were unable to find any other answer.
     */
    private static final Answer DEFAULT_ANSWER =
            new Answer(new Text("I don't know"), "");
    private IndexReader indexReader_;

    private QueryFormulation[] queryFormulator_;
    private SimilarQuestionAnswerRetrieval answerRetrieval_ =
            new SimilarQuestionAnswerRetrieval(queryFormulator_,
                    TOPN_SIMILAR_QUESTIONS);
    private AnswerSelection answerRanker_;

    public YahooAnswersBasedQuestionAnswerer(
            IndexReader indexReader, String modelPath) throws IOException {
        indexReader_ = indexReader;
        answerRanker_ = new FeatureBasedAnswerSelector(modelPath,
                getFeatureGenerator());
        queryFormulator_ = new QueryFormulation[]{
                new SimpleQueryFormulator(true, false),
                new SimpleQueryFormulator(false, false),
                new SimpleQueryFormulator(true, true),
                new SimpleQueryFormulator(false, true),
                new TopIdfTermsQueryFormulator(indexReader_, true, 0.5),
                new TopIdfTermsQueryFormulator(indexReader_, false, 0.5),
        };
    }

    @Override
    public Answer GetAnswer(Question question) {
        return answerRanker_.chooseBestAnswer(
                question, answerRetrieval_.retrieveAnswers(question))
                .orElse(YahooAnswersBasedQuestionAnswerer.DEFAULT_ANSWER);
    }

    public FeatureGeneration getFeatureGenerator() throws IOException {
        return new CombinerFeatureGenerator(
                //new LemmaPairsFeatureGenerator(),
                new MatchesFeatureGenerator(),
                new BM25FeatureGenerator(indexReader_),
                //new NamedEntityTypesFeatureGenerator(),
                // new ReverbTriplesFeatureGenerator(reverbIndexLocation),
                new AnswerStatsFeatureGenerator());
    }
}
