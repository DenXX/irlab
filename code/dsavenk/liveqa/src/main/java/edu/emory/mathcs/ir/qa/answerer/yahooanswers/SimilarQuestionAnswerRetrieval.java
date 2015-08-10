package edu.emory.mathcs.ir.qa.answerer.yahooanswers;

import edu.emory.mathcs.ir.qa.answer.Answer;
import edu.emory.mathcs.ir.qa.answerer.AnswerFormatter;
import edu.emory.mathcs.ir.qa.answerer.AnswerRetrieval;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.question.Question;
import edu.emory.mathcs.ir.qa.text.Text;
import edu.emory.mathcs.ir.scraping.YahooAnswersScraper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Uses Yahoo! Answers similar questions search to retrieve the answers to
 * related questions.
 */
public class SimilarQuestionAnswerRetrieval implements AnswerRetrieval {
    private final QueryFormulation queryFormulator_;
    private final int similarQuestionsCount_;

    /**
     * Creates an instance of similar questions answer retrieval.
     * @param queryFormulator Used to create queries for the provided questions.
     * @param similarQuestionsCount The number of similar questions to consider.
     */
    public SimilarQuestionAnswerRetrieval(
            final QueryFormulation queryFormulator, int similarQuestionsCount) {
        queryFormulator_ = queryFormulator;
        similarQuestionsCount_ = similarQuestionsCount;
    }

    @Override
    public Answer[] retrieveAnswers(Question question) {
        final String query = queryFormulator_.getQuery(question);
        final String[] relatedQuestionIds =
                YahooAnswersScraper.GetRelatedQuestionIds(query);
        List<Answer> bestRelatedAnswers = new ArrayList<>();

        // Get over the list of related questions and retrieve their best
        // answers.
        int index = 0;
        for (String qid : relatedQuestionIds) {
            // We only need to take top similarQuestionsCount_ questions.
            if (index++ > similarQuestionsCount_) break;
            YahooAnswersScraper.GetQuestionAnswerData(qid)
                    .ifPresent(qa -> {
                        final String url =
                                YahooAnswersScraper.GetQuestionAnswerUrl(qid);
                        String answer = qa.bestAnswer;
                        if (answer.isEmpty() && qa.answers.length > 0) {
                            answer = qa.answers[0];
                        }
                        if (!answer.isEmpty()) {
                            answer = AnswerFormatter.formatAnswer(answer);
                            bestRelatedAnswers.add(
                                    new Answer(new Text(answer), url));
                        }
                    });
        }
        return bestRelatedAnswers.toArray(
                new Answer[bestRelatedAnswers.size()]);
    }
}
