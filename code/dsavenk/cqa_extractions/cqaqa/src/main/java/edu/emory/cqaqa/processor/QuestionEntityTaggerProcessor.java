package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.CqaPost;

import edu.stanford.nlp.ie.AbstractSequenceClassifier;
import edu.stanford.nlp.ie.crf.CRFClassifier;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.HasWord;
import edu.stanford.nlp.process.DocumentPreprocessor;

import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Detects named entities in questions and creates questions templates.
 */
public class QuestionEntityTaggerProcessor implements CqaPostProcessor {
    private AbstractSequenceClassifier<CoreLabel> nerTagger;

    public QuestionEntityTaggerProcessor() throws IOException, ClassNotFoundException {
        nerTagger = CRFClassifier.getClassifier("edu/stanford/nlp/models/ner/english.muc.7class.distsim.crf.ser.gz");
    }

    private List<CoreLabel> getQuestionTemplate(List<HasWord> text) {
        List<CoreLabel> questionTemplate = new ArrayList<CoreLabel>();

        List<CoreLabel> labels = nerTagger.classifySentence(text);
        boolean isEntityQuestion = false;
        String lastLabelStr = "O";
        CoreLabel lastLabel = null;
        StringBuilder entityText = new StringBuilder();
        for (CoreLabel label : labels) {
            String strLabel = label.get(CoreAnnotations.AnswerAnnotation.class);
            label.setNER(strLabel);
            label.setWord(label.word().toLowerCase());
            // If we finished processing an entity, add a single token to the template.
            if (!lastLabelStr.equals(strLabel) && !lastLabelStr.equals("O")) {
                lastLabel.setWord(entityText.toString() + ">");
                questionTemplate.add(lastLabel);
                entityText = new StringBuilder();
            }

            if (!strLabel.equals("O")) {
                if (entityText.length() == 0) {
                    entityText.append("<");
                }
                else if (entityText.length() > 1) {
                    entityText.append(" ");
                }
                entityText.append(label.word());
                isEntityQuestion = true;
            } else {
                questionTemplate.add(label);
            }
            lastLabelStr = strLabel;
            lastLabel = label;
        }
        if (isEntityQuestion) {
            if (!lastLabelStr.equals("O")) {
                lastLabel.setOriginalText(entityText.toString() + ">");
                questionTemplate.add(lastLabel);
            }
            return questionTemplate;
        }
        return null;
    }

    @Override
    public CqaPost processPost(CqaPost post) {
        String question = post.getQuestion();
        DocumentPreprocessor preprocessor = new DocumentPreprocessor(new StringReader(question));
        List<CoreLabel> questionTemplate = null;
        int sentences = 0;
        for (List<HasWord> sentence : preprocessor) {
            // If more than one sentence
            if (sentences++ > 0) {
                return null;
            }
            questionTemplate = getQuestionTemplate(sentence);
        }
        if (questionTemplate == null) {
            return null;
        } else {
            post.setQuestionTokens(questionTemplate);
            return post;
        }
    }

}
