package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;

import java.util.Optional;
import java.util.Set;


/**
 * Created by dsavenk on 8/14/15.
 */
public class QuestionTermsCountAnswerSelector implements AnswerSelection {

    @Override
    public Optional<Answer> chooseBestAnswer(Question question,
                                             Answer[] answers) {
        Set<String> questionTitleTerms = question.getTitle().getLemmaSet();
        Set<String> questionBodyTerms = question.getBody().getLemmaSet();

        // Find the best candidate answer.
        double bestScore = Double.NEGATIVE_INFINITY;
        Optional<Answer> res = Optional.empty();

        // Score all answers and find the best candidate.
        for (final Answer answer : answers) {
            final double score = 1.0 * getScore(answer, questionTitleTerms) +
                    0.5 * getScore(answer, questionBodyTerms);
            if (score > bestScore) {
                res = Optional.of(answer);
                bestScore = score;
            } else if (score == bestScore && res.isPresent() &&
                    answer.getAnswer().text.length() >
                            res.get().getAnswer().text.length()) {
                res = Optional.of(answer);
            }
        }
        return res;
    }

    /**
     * Returns the score of a candidate answer based on how many question terms
     * occur in the answer.
     * @param answer The answer to score.
     * @param terms Question terms to count in the answer.
     * @return The score, which is the number of question terms we found in the
     * answer.
     */
    private double getScore(Answer answer, Set<String> terms) {
        Set<String> answerTerms = answer.getAnswer().getLemmaSet();
        answerTerms.retainAll(terms);
        return answerTerms.size();
    }
}
