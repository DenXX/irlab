package edu.emory.cqaqa.types;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;

import java.util.ArrayList;
import java.util.List;

/**
 * Stores CQA post information.
 */
public class CqaPost {
    private String qid = "";
    private String question = "";
    private List<CoreLabel> questionTokens = new ArrayList<CoreLabel>();
    private String details = "";
    private String bestAnswer = "";
    private String category = "";
    private String mainCategory = "";
    private String language = "";

    /**
     * Creates a CqaPost.
     * @param qid Question id.
     * @param question Question text.
     * @param details Question details.
     * @param bestAnswer Best answer text.
     * @param category Question category.
     * @param mainCategory Question main category.
     */
    public CqaPost(String qid, String language, String question, String details,
                   String bestAnswer, String category, String mainCategory) {
        this.setId(qid);
        this.setQuestion(question);
        this.setLanguage(language);
        this.setDetails(details);
        this.setBestAnswer(bestAnswer);
        this.setCategory(category);
        this.setMainCategory(mainCategory);
    }

    /**
     * Creates empty CqaPost.
     */
    public CqaPost() {

    }

    public String getId() {
        return this.qid;
    }

    public String getQuestion() {
        return this.question;
    }

    public String getDetails() {
        return details;
    }

    public String getBestAnswer() {
        return bestAnswer;
    }

    public String getCategory() {
        return category;
    }

    public String getMainCategory() {
        return mainCategory;
    }

    public void setId(String qid) {
        this.qid = qid;
    }

    public void setQuestion(String question) {
        this.question = question;
    }

    public void setDetails(String details) {
        this.details = details;
    }

    public void setBestAnswer(String bestAnswer) {
        this.bestAnswer = bestAnswer;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public void setMainCategory(String mainCategory) {
        this.mainCategory = mainCategory;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<CoreLabel> getQuestionTokens() {
        return questionTokens;
    }

    public void setQuestionTokens(List<CoreLabel> questionTokens) {
        this.questionTokens = questionTokens;
    }

    @Override
    public boolean equals(final Object obj) {
        CqaPost post = (CqaPost)obj;
        if (post.getQuestionTokens().size() != this.getQuestionTokens().size()) {
            return false;
        }
        for (int i = 0; i < this.getQuestionTokens().size(); ++i) {
            String ner1 = this.getQuestionTokens().get(i).ner();
            String ner2 = post.getQuestionTokens().get(i).ner();
            if (!ner1.equals(ner2)) {
                return false;
            }
            if (ner1.equals("O") &&
                    !this.getQuestionTokens().get(i).word().equals(post.getQuestionTokens().get(i).word())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        StringBuilder str = new StringBuilder();
        for (int i = 0; i < questionTokens.size(); ++i) {
            if (i > 0) {
                str.append(" ");
            }
            if (questionTokens.get(i).ner().equals("O")) {
                str.append(questionTokens.get(i).word());
            } else {
                str.append(questionTokens.get(i).ner());
            }
        }
        return str.toString().hashCode();
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        int index = 0;
        for (CoreLabel token : this.getQuestionTokens()) {
            if (index++ > 0) {
                res.append(" ");
            }
            if (token.ner().equals("O")) {
                res.append(token.word());
            } else {
                res.append(token.ner());
            }
        }
        res.append(" (" + question + ")");
        return res.toString();
    }
}
