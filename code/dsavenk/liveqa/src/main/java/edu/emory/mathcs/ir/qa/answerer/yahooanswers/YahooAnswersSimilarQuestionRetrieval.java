package edu.emory.mathcs.ir.qa.answerer.yahooanswers;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.qa.answerer.AnswerFormatter;
import edu.emory.mathcs.ir.qa.answerer.AnswerRetrieval;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.scraping.YahooAnswersScraper;

import java.util.ArrayList;
import java.util.List;

/**
 * Uses Yahoo! Answers similar questions search to retrieve the answers to
 * related questions.
 */
public class YahooAnswersSimilarQuestionRetrieval implements AnswerRetrieval {
    private final QueryFormulation[] queryFormulators_;
    private final int similarQuestionsCount_;

    /**
     * Creates an instance of similar questions answer retrieval.
     * @param queryFormulators List of query formulators that are used to create
     *                         queries for the provided questions.
     * @param similarQuestionsCount The number of similar questions to consider.
     */
    public YahooAnswersSimilarQuestionRetrieval(
            final QueryFormulation[] queryFormulators,
            int similarQuestionsCount) {
        queryFormulators_ = queryFormulators;
        similarQuestionsCount_ = similarQuestionsCount;
    }

    @Override
    public Answer[] retrieveAnswers(Question question) {
        final List<Answer> bestRelatedAnswers = new ArrayList<>();
        for (QueryFormulation queryFormulator : queryFormulators_) {
            final String query = queryFormulator.getQuery(question);

            final String[] relatedQuestionIds =
                    YahooAnswersScraper.GetRelatedQuestionIds(
                            query, similarQuestionsCount_);

            // Get over the list of related questions and retrieve their best
            // answers.
            int index = 0;
            for (String qid : relatedQuestionIds) {
                // Skip retrieved answer if it is the same as the query.
                if (question.getId().equals(qid)) continue;

                // We only need to take top similarQuestionsCount_ questions.
                if (index++ > similarQuestionsCount_) break;
                YahooAnswersScraper.GetQuestionAnswerData(qid)
                        .ifPresent(qa -> {
                            final String url =
                                    YahooAnswersScraper.GetQuestionAnswerUrl(
                                            qid);
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
        }
        return bestRelatedAnswers.toArray(
                new Answer[bestRelatedAnswers.size()]);
    }
}
