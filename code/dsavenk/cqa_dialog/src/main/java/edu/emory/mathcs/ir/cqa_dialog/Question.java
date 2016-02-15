package edu.emory.mathcs.ir.cqa_dialog;

import edu.emory.mathcs.ir.cqa_dialog.utils.NlpUtils;
import edu.stanford.nlp.simple.Document;
import edu.stanford.nlp.simple.Sentence;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by dsavenk on 2/10/16.
 */
public class Question {
    private int id;
    private String title;
    private String body;
    private Document fullQuestionNlp = null;
    private List<String> comments = new ArrayList<>();
    private String answer;

    public Question(int id, String title, String body) {
        this.id = id;
        this.title = cleanUp(title);
        this.body = cleanUp(body);
    }

    public String getTitle() {
        return title;
    }

    public String getBody() {
        return body;
    }

    public int getId() {
        return id;
    }

    public void addComment(String comment) {
        this.comments.add(cleanUp(comment));
    }

    public String[] getComments() {
        return this.comments.toArray(new String[this.comments.size()]);
    }

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = cleanUp(answer);
    }

    private String cleanUp(String text) {
        return text.replaceAll("<[^>]*>", " ");
    }

    public boolean hasCommentContaining(String substr) {
        for (String comment : comments) {
            if (comment.toLowerCase().contains(substr.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    public String[] getQuestionNounPhrases() {
        List<String> nps = new ArrayList<>();
        if (fullQuestionNlp == null) {
            fullQuestionNlp = new edu.stanford.nlp.simple.Document(
                    this.title + "\n" + this.body);
        }
        for (Sentence sent : fullQuestionNlp.sentences()) {
            nps.addAll(Arrays.asList(NlpUtils.extractNp(sent, false)));
        }
        return nps.toArray(new String[nps.size()]);
    }
}
