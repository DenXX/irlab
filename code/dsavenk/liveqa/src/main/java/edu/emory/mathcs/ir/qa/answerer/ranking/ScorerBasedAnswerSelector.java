package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.Optional;

/**
 * Selects best answer from the given list using the provided answer scorer to
 * rank the candidates.
 */
public class ScorerBasedAnswerSelector implements AnswerSelection {
    private AnswerScoring answerScorer_;

    /**
     * Creates answer selector.
     *
     * @param answerScorer The candidate answer scorer to use for ranking.
     */
    public ScorerBasedAnswerSelector(AnswerScoring answerScorer) {
        answerScorer_ = answerScorer;
    }

    @Override
    public Optional<Answer> chooseBestAnswer(
            Question question, Answer[] answers) {
        double bestScore = Double.NEGATIVE_INFINITY;
        Optional<Answer> bestAnswer = Optional.empty();
        for (Answer answer : answers) {
            double score = answerScorer_.scoreAnswer(question, answer);
            if (score > bestScore) {
                bestAnswer = Optional.of(answer);
                bestScore = score;
            }
        }
        return bestAnswer;
    }
}
