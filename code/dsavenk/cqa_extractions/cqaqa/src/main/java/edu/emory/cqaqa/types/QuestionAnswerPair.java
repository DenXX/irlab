package edu.emory.cqaqa.types;

import com.hp.hpl.jena.tdb.store.Hash;
import edu.emory.cqaqa.tools.Freebase;
import edu.emory.cqaqa.utils.NlpUtils;
import edu.stanford.nlp.ling.CoreLabel;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dsavenk on 4/30/14.
 */
public class QuestionAnswerPair {
    private String id;
    private String rawQuestion;
    private List<List<CoreLabel>> question;
    private String rawAnswer;
    private List<List<CoreLabel>> answer;
    Map<String, String> attributes = new HashMap<String, String>();  // Stores other attributes of question answer pair.

    /**
     * Creates empty question with the given id.
     * @param id
     */
    public QuestionAnswerPair() {
    }


    public String getQuestion() {
        return rawQuestion;
    }

    public void setQuestion(String rawQuestion) {
        this.rawQuestion = rawQuestion;
        this.question = NlpUtils.detectEntities(rawQuestion);
    }

    public String getRawAnswer() {
        return rawAnswer;
    }

    public void setAnswer(String rawAnswer) {
        this.rawAnswer = rawAnswer;
        this.answer = NlpUtils.detectEntities(rawAnswer);
    }

    public void addAttribute(String name, String value) {
        attributes.put(name, value);
    }

    public String getAttribute(String name) {
        return attributes.get(name);
    }

    public List<List<CoreLabel>> getAnswerTokens() {
        return answer;
    }

    public List<List<CoreLabel>> getQuestionTokens() {
        return question;
    }

    public boolean hasAttribute(String name) {
        return attributes.containsKey(name);
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        int index = 0;
        for (List<CoreLabel> sentence : question) {
            for (CoreLabel word : sentence) {
                if (index++ > 0) res.append(" ");
                res.append(word.word() + "[" + word.ner() + "," + word.get(FreebaseEntityAnnotation.class) + "]");
            }
        }
        res.append("\n=== Answer: ===\n");
        index = 0;
        for (List<CoreLabel> sentence : answer) {
            for (CoreLabel word : sentence) {
                if (index++ > 0) res.append(" ");
                res.append(word.word() + "[" + word.ner() + "," + word.get(FreebaseEntityAnnotation.class) + "]");
            }
        }
        return res.toString();
    }

}
