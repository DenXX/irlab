package edu.emory.mathcs.ir.qa.answerer;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerSelection;
import edu.emory.mathcs.ir.scraping.YahooAnswersScraper;
import org.apache.lucene.index.IndexReader;

/**
 * Combination between CQA-based and Web question answering.
 */
public class YahooAnswersAndWebQuestionsAnswerer implements QuestionAnswering {
    final GenericQuestionAnswerer yaAnswerer_;

    public YahooAnswersAndWebQuestionsAnswerer(
            IndexReader reader, AnswerSelection bestAnswerSelector) {

    }

    @Override
    public Answer GetAnswer(Question question) {
        // Get all question categories, not only the selected one.
        question.setCategories(
                YahooAnswersScraper.getQuestionCategories(question));



        return null;
    }
}
