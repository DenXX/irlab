package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.utils.NlpUtils;
import edu.emory.mathcs.ir.utils.StringUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Generates queries for question by simply using their text.
 */
public class SimpleQueryFormulator implements QueryFormulation {
    private boolean includeBody_;
    private boolean removeStopwords_;

    public SimpleQueryFormulator(boolean includeBody, boolean removeStopwords) {
        includeBody_ = includeBody;
        removeStopwords_ = removeStopwords;
    }

    @Override
    public String getQuery(Question question) {
        Text text = question.getTitle();
        if (includeBody_) {
            text = text.concat(question.getBody());
        }
        return getQueryString(text);
    }

    private String getQueryString(Text text) {
        return Arrays.stream(text.getTokens())
                .filter(token -> Character.isAlphabetic(token.pos.charAt(0)))
                .filter(token -> !removeStopwords_ ||
                        !NlpUtils.getStopwords().contains(token.lemma))
                .map(token -> StringUtils.normalizeString(token.text))
                .collect(Collectors.joining(" ")).trim();
    }
}
