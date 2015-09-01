package edu.emory.mathcs.ir.qa.ml;

import com.google.common.collect.Sets;
import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Computes features based on NPMI scores between terms in the question and
 * terms in the answer.
 */
public class NpmiDictionaryMatchesFeatureGenerator implements FeatureGeneration {
    private Map<String, Map<String, Double>> npmiScores_ = new HashMap<>();

    /**
     * Creates feature generator and specifies the location of the dictionary
     * with the npmi word pair scores dictionary. The format of the file is
     * [question term]\t[answer term]\t[score]
     *
     * @param npmiDictLocation The location of the file with the npmi scores
     *                         dictionary.
     * @throws IOException
     */
    public NpmiDictionaryMatchesFeatureGenerator(String npmiDictLocation)
            throws IOException {
        readNpmiDict(npmiDictLocation);
    }

    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        Set<String> titleTerms = question.getTitle().getLemmaSet(false);
        Set<String> bodyTerms = question.getTitle().getLemmaSet(false);
        Set<String> titleBodyTerms = Sets.union(titleTerms, bodyTerms);
        Map<String, Double> features = new HashMap<>();
        computeTextNpmiFeatures(titleTerms, answer, features, "title:");
        computeTextNpmiFeatures(bodyTerms, answer, features, "body:");
        computeTextNpmiFeatures(titleBodyTerms, answer, features, "all:");
        return features;
    }

    private void computeTextNpmiFeatures(
            Set<String> terms, Answer answer,
            Map<String, Double> features, String prefix) {
        double minNpmi = 0;
        double maxNpmi = 0;
        double maxNotSameTokenNpmi = 0;
        double avgNpmi = 0;
        double avgPositiveNpmi = 0;
        int numPositive = 0;
        int numNegative = 0;

        // Get answer tokens.
        Text.Token[] answerTokens = answer.getAnswer().getTokens();
        if (answerTokens.length == 0) return;

        for (String term : terms) {
            if (npmiScores_.containsKey(term)) {
                for (Text.Token token : answerTokens) {
                    if (npmiScores_.get(term).containsKey(token.lemma)) {
                        double score = npmiScores_.get(term).get(token.lemma);
                        if (score < 0) ++numNegative;
                        else if (score > 0) {
                            ++numPositive;
                            ++avgPositiveNpmi;
                        }
                        avgNpmi += score;
                        minNpmi = Math.min(minNpmi, score);
                        maxNpmi = Math.max(maxNpmi, score);

                        if (!term.equals(token.lemma)) {
                            maxNotSameTokenNpmi =
                                    Math.max(maxNotSameTokenNpmi, score);
                        }
                    }
                }
            }
        }

        features.put(prefix + "min_npmi=", minNpmi);
        features.put(prefix + "max_npmi=", maxNpmi);
        features.put(prefix + "max_notsametoken_npmi=", maxNotSameTokenNpmi);
        features.put(prefix + "avg_npmi=", avgNpmi / answerTokens.length);
        features.put(prefix + "%_negative_npmi=",
                1.0 * numNegative / answerTokens.length);
        features.put(prefix + "%_positive_npmi=",
                1.0 * numPositive / answerTokens.length);
        features.put(prefix + "%_zero_npmi=",
                1.0 * (answerTokens.length - numPositive - numNegative) /
                        answerTokens.length);
        if (numPositive > 0) {
            features.put(prefix + "%_avg_positive_npmi=",
                    1.0 * avgPositiveNpmi / numPositive);
        }
    }

    private void readNpmiDict(String npmiDictLocation) throws IOException {
        BufferedReader input = new BufferedReader(
                new InputStreamReader(new FileInputStream(npmiDictLocation)));
        String line;
        while ((line = input.readLine()) != null) {
            String[] fields = line.split("\t");
            if (!npmiScores_.containsKey(fields[0])) {
                npmiScores_.put(fields[0], new HashMap<>());
            }
            npmiScores_.get(fields[0]).put(fields[1],
                    Double.parseDouble(fields[2]));
        }
        input.close();
    }
}
