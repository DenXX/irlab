package edu.emory.mathcs.ir.qa.ml;

import com.google.common.collect.Sets;
import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.Text;
import edu.emory.mathcs.ir.qa.answerer.web.WebSearchAnswerRetrieval;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Feature generator that outputs the feature based on term matches between
 * question terms and answer web page title.
 */
public class PageTitleMatchFeatureGenerator implements FeatureGeneration {
    @Override
    public Map<String, Double> generateFeatures(
            Question question, Answer answer) {
        Map<String, Double> features = new HashMap<>();
        Optional<String> title = answer.getAttribute(
                WebSearchAnswerRetrieval.PAGE_TITLE_ATTRIBUTE);
        if (title.isPresent()) {
            Text pageTitle = new Text(title.get());
            Set<String> pageTitleTerms = pageTitle.getLemmaSet(true);

            outputTitleMatchFeatures(question.getTitle(), pageTitleTerms,
                    features, "title:");
            outputTitleMatchFeatures(question.getBody(), pageTitleTerms,
                    features, "body:");
        }
        return features;
    }

    private void outputTitleMatchFeatures(
            Text question, Set<String> pageTitleTerms,
            Map<String, Double> features, String prefix) {
        if (pageTitleTerms.size() == 0) return;

        int intersection =
                Sets.union(question.getLemmaSet(true), pageTitleTerms).size();
        if (intersection > 0) {
            features.put(prefix + "page_title_matches=",
                    1.0 * intersection / pageTitleTerms.size());
        } else {
            features.put(prefix + "page_title_no_matches", 1.0);
        }
    }
}
