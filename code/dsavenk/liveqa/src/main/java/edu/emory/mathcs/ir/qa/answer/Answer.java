package edu.emory.mathcs.ir.qa.answer;

import edu.emory.mathcs.ir.qa.text.Text;
import org.checkerframework.checker.nullness.qual.NonNull;

/**
 * Stores the answer to a question along with some meta-information.
 */
public class Answer {
    // Stores the text representation of the answer.
    private final Text answerText_;

    // Source of the answer, e.g. URL of a webpage.
    private final String source_;

    /**
     * Creates the answer with the given text.
     * @param answer Text representation of the answer.
     */
    public Answer(@NonNull Text answer, @NonNull String source) {
        answerText_ = answer;
        source_ = source;
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
}
