package edu.emory.mathcs.ir.utils;

import edu.emory.mathcs.ir.qa.Text;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.AnnotationPipeline;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import java.util.Properties;

/**
 * Provides a set of natural language processing utils.
 */
public class NlpUtils {
    private static final AnnotationPipeline nlpPipeline_ =
            new StanfordCoreNLP(getProperties(), true);

    private static Properties getProperties() {
        Properties properties = new Properties();
        properties.setProperty("annotators",
                "tokenize, ssplit, pos, lemma"); // ner, entitymentions
        properties.setProperty("ssplit.newlineIsSentenceBreak", "always");
        return properties;
    }

    /**
     * Returns text annotated with Stanford CoreNLP pipeline.
     * @param text Text to annotate.
     * @return Text annotations.
     */
    public static Annotation getAnnotations(String text) {
        Annotation textAnnotation = new Annotation(text);
        nlpPipeline_.annotate(textAnnotation);
        return textAnnotation;
    }
}
