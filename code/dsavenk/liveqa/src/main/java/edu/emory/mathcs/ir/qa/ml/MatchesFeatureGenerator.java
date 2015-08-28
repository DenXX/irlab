package edu.emory.mathcs.ir.qa.ml;

import com.google.common.collect.Sets;
import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by dsavenk on 8/25/15.
 */
public class MatchesFeatureGenerator implements FeatureGeneration {
    @Override
    public Map<String, Double> generateFeatures(Question question,
                                                Answer answer) {
        Set<String> titleTerms = question.getTitle().getLemmaSet(true);
        Set<String> titleAndBodyTerms = Sets.union(titleTerms,
                question.getBody().getLemmaSet(true));

        Map<String, Double> features = new HashMap<>();
        outputMatchFeatures(titleTerms, answer.getAnswer(), "title:", features);
        outputMatchFeatures(titleAndBodyTerms,
                answer.getAnswer(), "all:", features);

        return features;
    }

    private void outputMatchFeatures(Set<String> terms, Text passage,
                                     String featurePrefix,
                                     Map<String, Double> features) {
        if (terms.isEmpty()) return;

        int totalMatches = 0;
        Set<String> matchedTerms = new HashSet<>();
        Map<String, Integer> posMatchCounts = new HashMap<>();
        int maximumMatchSpan = 0;
        for (Text.Sentence sentence : passage.getSentences()) {
            int matchSpanLength = 0;
            for (Text.Token token : sentence.tokens) {
                if (terms.contains(token.lemma)) {
                    ++totalMatches;
                    ++matchSpanLength;
                    posMatchCounts.putIfAbsent(token.pos, 0);
                    posMatchCounts.put(token.pos,
                            posMatchCounts.get(token.pos) + 1);
                    if (!matchedTerms.contains(token.lemma)) {
                        matchedTerms.add(token.lemma);
                    }
                    features.put(
                            featurePrefix + "matched_term=" + token.lemma, 1.0);
                } else {
                    maximumMatchSpan = Math.max(maximumMatchSpan,
                            matchSpanLength);
                    matchSpanLength = 0;
                }
            }
            maximumMatchSpan = Math.max(maximumMatchSpan, matchSpanLength);
        }
        features.put(featurePrefix + "total_matches=", (double) totalMatches);
        features.put(featurePrefix + "matches_span_length=",
                (double) maximumMatchSpan);
        features.put(featurePrefix + "%_matched_terms=",
                (double) matchedTerms.size() / terms.size());

        for (Map.Entry<String, Integer> entry : posMatchCounts.entrySet()) {
            features.put(featurePrefix + "matched_terms_pos=" + entry.getKey()
                    + "=", Double.valueOf(entry.getValue()));
        }
    }
}
