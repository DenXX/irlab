package edu.emory.mathcs.ir.qa;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 * Represents a question to be answered.
 */
public class Question {
    private String id_;
    private Text title_;
    private Text body_;
    private String category_;
    private String[] categories_;
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
        categories_ = new String[] {category};
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

    /**
     * @return Returns categories of the question. Unlike the
     * {@link Question::getCategory} method, the categories also contain main
     * and subcategories of the question.
     */
    public String[] getCategories() {
        return categories_;
    }

    /**
     * Sets the categories of the given question.
     * @param categories The categories of the question.
     */
    public void setCategories(String[] categories) {
        categories_ = categories;
    }
}
