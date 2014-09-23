package edu.emory.mathcs.clir.relextract.data;

import edu.stanford.nlp.pipeline.Annotation;

import java.util.HashMap;

/**
 * A class that stores Question and Answer pair as a document for NLP pipeline
 * annotation.
 */
public class QuestionAnswerAnnotation extends Annotation {

    /**
     * Creates a new Q&A instance from question and answer texts.
     * @param question Question text.
     * @param answer Answer text.
     */
    public QuestionAnswerAnnotation(String question, String answer) {
        super(question + " " + answer);
        questionLength_ = question.length();
    }

    /**
     * Adds an attribute to the given Q&A pair. Attribute is any
     * meta-information we might have on the current Q&A pair.
     * @param name The name of the attribute.
     * @param value The value of the attribute.
     */
    public void addAttribute(String name, String value) {
        attributes_.put(name, value);
    }

    // Stores the length of the question in chars. We can then determine whether
    // a particular term belong to the question or answer text.
    private int questionLength_;
    private HashMap<String, String> attributes_ = new HashMap<>();
}
