package edu.emory.mathcs.ir.qa.question;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;
import edu.emory.mathcs.ir.qa.text.Text;

/**
 * Represents a question to be answered.
 */
public class Question {
    private String id_;
    private Text title_;
    private Text body_;
    private String category_;
    private ClassToInstanceMap<QuestionAnnotation> annotations_ =
            MutableClassToInstanceMap.create();

    /**
     * Creates a Question from the given id, title, body and question category.
     * @param id Question id.
     * @param title Question title.
     * @param body Question body.
     * @param category Question category.
     */
    public Question(String id, String title, String body, String category) {
        id_ = id;
        title_ = new Text(title);
        body_ = new Text(body);
        category_ = category;
    }

    /**
     * @return The text representation of the question title.
     */
    public Text getTitle() {
        return title_;
    }

    /**
     * @return The text representation of the question body.
     */
    public Text getBody() {
        return body_;
    }

    /**
     * @return The id of the question.
     */
    public String getId() {
        return id_;
    }

    /**
     * @return The category of the question.
     */
    public String getCategory() {
        return category_;
    }

    @Override
    public String toString() {
        return String.join("\t", new String[] {
                getId(), getCategory(), getTitle().toString(),
                getBody().toString()});
    }
}
