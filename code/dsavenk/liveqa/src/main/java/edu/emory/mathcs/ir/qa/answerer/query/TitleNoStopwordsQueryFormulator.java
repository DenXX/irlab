package edu.emory.mathcs.ir.qa.answerer.query;

import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.utils.NlpUtils;

import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 8/24/15.
 */
public class TitleNoStopwordsQueryFormulator implements QueryFormulation {
    @Override
    public String getQuery(Question question) {
        String query = removeStopwords(question.getTitle());
        if (query.isEmpty()) {
            query = removeStopwords(question.getBody());
        }
        return query;
    }

    private String removeStopwords(Text text) {
        return Arrays.stream(text.getTokens())
                .filter(token -> !NlpUtils.getStopwords().contains(token.lemma)
                        && Character.isAlphabetic(token.pos.charAt(0)))
                .map(token -> token.lemma)
                .collect(Collectors.joining(" ")).trim();
    }
}
