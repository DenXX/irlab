package edu.emory.mathcs.ir.qa.answerer.yahooanswers;

import edu.emory.mathcs.ir.qa.answer.Answer;
import edu.emory.mathcs.ir.qa.answerer.QuestionAnswering;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.answerer.query.TitleOnlyQueryFormulator;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerSelection;
import edu.emory.mathcs.ir.qa.answerer.ranking.TopAnswerSelector;
import edu.emory.mathcs.ir.qa.question.Question;
import edu.emory.mathcs.ir.qa.text.Text;

/**
 * QA based on the best answer to the related questions from Yahoo! Answers.
 * Related questions are obtained using Yahoo! Answers search.
 */
public class YahooAnswersBasedQuestionAnswerer implements QuestionAnswering {
    /**
     * The number of top similar questions to extract answers from.
     */
    public static final int TOPN_SIMILAR_QUESTIONS = 1;

    /**
     * Default answer returned in case we were unable to find any other answer.
     */
    private static final Answer DEFAULT_ANSWER =
            new Answer(new Text("I don't know"), "");

    private QueryFormulation queryFormulator_ = new TitleOnlyQueryFormulator();
    private SimilarQuestionAnswerRetrieval answerRetrieval_ =
            new SimilarQuestionAnswerRetrieval(queryFormulator_,
                    TOPN_SIMILAR_QUESTIONS);
    private AnswerSelection answerRanker_ = new TopAnswerSelector();

    @Override
    public Answer GetAnswer(Question question) {
        return answerRanker_.chooseBestAnswer(
                question, answerRetrieval_.retrieveAnswers(question))
                .orElse(YahooAnswersBasedQuestionAnswerer.DEFAULT_ANSWER);
    }
}
