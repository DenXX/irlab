package edu.emory.mathcs.ir.qa;

import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Stores the answer to a question along with some meta-information.
 */
public class Answer {
    // Stores the text representation of the answer.
    private final Text answerText_;

    // Source of the answer, e.g. URL of a webpage.
    private final String source_;

    // Some attributes of the answer.
    // TODO(denxx): Refactor this, need to have a better way to query and set
    // this attributes.
    private final Map<String, String> attributes_ = new HashMap<>();

    /**
     * Creates the answer with the given text.
     * @param answer Text representation of the answer.
     * @param source The source of the answer.
     */
    public Answer(@NonNull Text answer, @NonNull String source) {
        answerText_ = answer;
        source_ = source;
    }

    /**
     * Creates the answer with the given text and source.
     * @param answer String containing the answer.
     * @param source The source of the answer.
     */
    public Answer(@NonNull String answer, @NonNull String source) {
        this(new Text(answer), source);
    }

    /**
     * @return Returns the text representation of the answer.
     */
    public Text getAnswer() {
        return answerText_;
    }

    /**
     * @return Returns the source of the answer.
     */
    public String getSource() {
        return source_;
    }

    /**
     * Returns the attribute with the given name if present or empty.
     *
     * @param attribute The name of the attribute to return.
     * @return The value of the attribute or empty.
     */
    public Optional<String> getAttribute(String attribute) {
        return attributes_.containsKey(attribute) ?
                Optional.of(attributes_.get(attribute)) :
                Optional.empty();
    }

    /**
     * Sets the value of the attribute with the given name.
     *
     * @param attribute The name of the attribute.
     * @param value     The value of the attribute.
     */
    public void setAttribute(String attribute, String value) {
        attributes_.put(attribute, value);
    }

    @Override
    public String toString() {
        return String.join("\t", new String[] {
                getAnswer().text, getSource()});
    }
}
