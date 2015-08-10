package edu.emory.mathcs.ir.qa.text;

/**
 * Stores a text with NLP annotations, i.e. sentence splits, tokens, POS, etc.
 */
public class Text {
    /**
     * Stores the string representation of the text.
     */
    public final String text;

    /**
     * Creates an annotated text from its string representation.
     * @param text
     */
    public Text(String text) {
        this.text = text;
        Annotate();
    }

    /**
     * Annotates the text with an additional information.
     */
    private void Annotate() {}

    @Override
    public String toString() {
        return text;
    }
}
