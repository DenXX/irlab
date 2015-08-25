package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.input.YahooAnswersXmlInput;
import edu.emory.mathcs.ir.qa.Question;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Created by dsavenk on 8/25/15.
 */
public class ComputeQATermNpmi {
    public static void main(String[] args) {
        final String dataLocation = args[0];

        Map<String, Long> questionTermCount = new HashMap<>();
        Map<String, Long> answerTermCount = new HashMap<>();
        Map<WordPair, Long> termPairsCount = new HashMap<>();
        Map<WordPair, Double> termPairNpmi = new HashMap<>();

        YahooAnswersXmlInput input = new YahooAnswersXmlInput(args[0]);

        long index = 0;
        for (final YahooAnswersXmlInput.QnAPair qna : input) {
            final Question question = qna.getQuestion();
            final Set<String> questionTerms =
                    question.getTitle().getLemmaSet(true);
            questionTerms.addAll(question.getBody().getLemmaSet(true));

            final Set<String> answerTerms =
                    qna.getAnswer().getAnswer().getLemmaSet(true);

            for (String questionTerm : questionTerms) {
                questionTermCount.putIfAbsent(questionTerm, 0L);
                questionTermCount.put(questionTerm,
                        questionTermCount.get(questionTerm) + 1);
            }

            for (String answerTerm : answerTerms) {
                answerTermCount.putIfAbsent(answerTerm, 0L);
                answerTermCount.put(answerTerm,
                        answerTermCount.get(answerTerm) + 1);
            }

            for (String questionTerm : questionTerms) {
                for (String answerTerm : answerTerms) {
                    WordPair wp = new WordPair(questionTerm, answerTerm);
                    termPairsCount.putIfAbsent(wp, 0L);
                    termPairsCount.put(wp, termPairsCount.get(wp) + 1);
                }
            }

            if (index % 1000 == 0) {
                System.err.println(index + " docs processed!");
            }
            ++index;
        }

        for (Map.Entry<WordPair, Long> wpCount :
                termPairsCount.entrySet()) {
            if (wpCount.getValue() > 10) {
                double npmi = Math.log(wpCount.getValue()) +
                        Math.log(index) -
                        Math.log(questionTermCount.get(
                                wpCount.getKey().first)) -
                        Math.log(answerTermCount.get(
                                wpCount.getKey().second));

                npmi /= Math.log(index) -
                        Math.log(wpCount.getValue());

                termPairNpmi.put(wpCount.getKey(), npmi);
                System.out.println(String.format("%s\t%s\t%.5f",
                        wpCount.getKey().first, wpCount.getKey().second,
                        npmi));
            }
        }
    }

    public static class WordPair {
        public String first;
        public String second;

        public WordPair(String first, String second) {
            this.first = first;
            this.second = second;
        }

        @Override
        public int hashCode() {
            return (first + "\t" + second).hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof WordPair &&
                    first.equals(((WordPair) obj).first) &&
                    second.equals(((WordPair) obj).second);
        }
    }
}
