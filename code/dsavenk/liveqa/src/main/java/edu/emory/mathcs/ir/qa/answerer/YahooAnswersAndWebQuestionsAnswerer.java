package edu.emory.mathcs.ir.qa.answerer;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.AppConfig;
import edu.emory.mathcs.ir.qa.LiveQaLogger;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.passage.SentenceBasedPassageRetrieval;
import edu.emory.mathcs.ir.qa.answerer.ranking.AnswerSelection;
import edu.emory.mathcs.ir.qa.answerer.web.WebSearchAnswerRetrieval;
import edu.emory.mathcs.ir.qa.answerer.yahooanswers.YahooAnswersSimilarQuestionRetrieval;
import edu.emory.mathcs.ir.scraping.YahooAnswersScraper;
import edu.emory.mathcs.ir.search.BingWebSearch;

import java.io.IOException;

/**
 * Combination between CQA-based and Web question answering.
 */
public class YahooAnswersAndWebQuestionsAnswerer implements QuestionAnswering {
    private final AnswerRetrieval answerRetrieval_;
    private final AnswerSelection answerSelector_;


    public YahooAnswersAndWebQuestionsAnswerer(AnswerSelection answerSelector)
            throws IOException {
        answerRetrieval_ = new CombinerAnswerRetrieval(
                getYaAnswerRetrieval(),
                getWebAnswerRetrieval()
        );
        answerSelector_ = answerSelector;
    }

    private AnswerRetrieval getWebAnswerRetrieval() {
        return new WebSearchAnswerRetrieval(AppConfig.getWebQueryFormulators(),
                new BingWebSearch(), new SentenceBasedPassageRetrieval());
    }

    private AnswerRetrieval getYaAnswerRetrieval() {
        return new YahooAnswersSimilarQuestionRetrieval(
                AppConfig.getYaQueryFormulators(),
                Integer.parseInt(AppConfig.PROPERTIES.getProperty(
                        AppConfig.SIMILAR_QUESTIONS_COUNT_PARAMETER)));
    }

    @Override
    public Answer GetAnswer(Question question) {
        LiveQaLogger.LOGGER.fine("------------- QUESTION ----------------");
        LiveQaLogger.LOGGER.fine("QID:\t" + question.getId());
        // Get all question categories, not only the selected one.
        question.setCategories(
                YahooAnswersScraper.getQuestionCategories(question));

        try {
            return answerSelector_.chooseBestAnswer(question,
                    answerRetrieval_.retrieveAnswers(question))
                    .orElse(getDefaultAnswer());
        } catch (Exception e) {
            return getDefaultAnswer();
        }
    }

    private Answer getDefaultAnswer() {
        return new Answer("I don't know :(", "");
    }
}
