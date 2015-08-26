package edu.emory.mathcs.ir.qa.answerer.ranking;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.ml.LemmaPairsFeatureGenerator;
import edu.stanford.nlp.classify.LinearClassifier;
import edu.stanford.nlp.stats.ClassicCounter;
import edu.stanford.nlp.stats.Counter;
import edu.stanford.nlp.util.Pair;
import junit.framework.TestCase;

import java.util.Optional;

/**
 * Created by dsavenk on 8/20/15.
 */
public class FeatureBasedAnswerSelectorTest extends TestCase {
    private LinearClassifier<Boolean, String> model_;

    public void setUp() {
        Counter<Pair<String, Boolean>> weights = new ClassicCounter<>();
        weights.setCount(new Pair<>("title:president_president", true), 1.0);
        weights.setCount(new Pair<>("title:president_president", false), -1.0);
        weights.setCount(new Pair<>("title:usa_usa", true), 1.0);
        weights.setCount(new Pair<>("title:usa_usa", false), -1.0);
        weights.setCount(new Pair<>("title:usa_russia", true), -1.0);
        weights.setCount(new Pair<>("title:usa_russia", false), 1.0);
        model_ = new LinearClassifier<>(weights);
    }

    public void testChooseBestAnswer() throws Exception {
        FeatureBasedAnswerSelector selector =
                new FeatureBasedAnswerSelector(model_,
                        new LemmaPairsFeatureGenerator());

        Question q = new Question(
                "", "Who is the president of the USA?", "I mean in 2015", "");
        Answer[] answers = new Answer[]{
                new Answer("The president of the Russia is Vladimir Putin", ""),
                new Answer("The president of the USA is Barack Obama", ""),
                new Answer("I think he had a cancer", ""),
        };
        final Optional<Answer> bestAnwer =
                selector.chooseBestAnswer(q, answers);
        assertTrue(bestAnwer.isPresent());
        assertTrue(bestAnwer.get().getAnswer().text.contains("Barack Obama"));
    }

    public void testChooseBestAnswerWithNoAnswers() throws Exception {
        FeatureBasedAnswerSelector selector =
                new FeatureBasedAnswerSelector(model_,
                        new LemmaPairsFeatureGenerator());

        Question q = new Question(
                "", "Who is the president of the US?", "I mean in 2015", "");
        Answer[] answers = new Answer[0];
        final Optional<Answer> bestAnwer =
                selector.chooseBestAnswer(q, answers);
        assertFalse(bestAnwer.isPresent());
    }
}
