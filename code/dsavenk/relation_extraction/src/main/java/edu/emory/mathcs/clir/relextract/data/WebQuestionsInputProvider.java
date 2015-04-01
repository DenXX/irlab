package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.AppParameters;
import org.json.simple.JSONObject;
import org.json.simple.JSONArray;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Created by dsavenk on 1/23/15.
 */
public class WebQuestionsInputProvider implements Iterable<Document.NlpDocument> {

    private final String input_;

    public WebQuestionsInputProvider(Properties props) {
        this.input_ = props.getProperty(AppParameters.INPUT_PARAMETER);
    }

    @Override
    public Iterator<Document.NlpDocument> iterator() {
        return new WebQuestionsIterator(input_);
    }

    public static class WebQuestionsIterator implements java.util.Iterator<Document.NlpDocument> {

        private final JSONParser parser_ = new JSONParser();
        private final List<Document.NlpDocument> documents_ = new ArrayList<>();
        private int currentDoc_ = 0;

        public WebQuestionsIterator(String inputPath) {
            try {
                BufferedReader inputReader = new BufferedReader(new FileReader(inputPath));
                JSONArray docs = (JSONArray) parser_.parse(inputReader);

                for (int i = 0; i < docs.size(); ++i) {
                    JSONObject doc = (JSONObject)docs.get(i);
                    String question = (String)doc.get("utterance");
                    String answer = extractAnswerValues((String) doc.get("targetValue"));

                    documents_.add(Document.NlpDocument.newBuilder()
                            .setText(question + "\n" + answer)
                            .setQuestionLength(question.length())
                            .setAnswerLength(answer.length())
                            .addPartsType(1)
                            .addPartsCharOffset(0)
                            .addPartsType(2)
                            .addPartsCharOffset(question.length() + 1)
                            .addAttribute(Document.Attribute.newBuilder().setKey("url").setValue((String)doc.get("url")))
                            .build());
                }
                inputReader.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private String extractAnswerValues(String answerLisp) {
            StringBuilder res = new StringBuilder();
            int pos = -1;

            while ((pos = answerLisp.indexOf("(description", pos + 1)) >= 0) {
                int space = answerLisp.indexOf(" ", pos);
                int quote = answerLisp.indexOf("\"", pos);
                int par = answerLisp.indexOf(")", pos);
                int beg = quote > 0 && quote < par ? quote : space;
                int end = quote > 0 && quote < par ? answerLisp.indexOf("\"", quote + 1) : par;
                res.append("<").append(answerLisp.substring(beg + 1, end)).append(">,\n");
                pos = end;
            }
            return res.toString();
        }

        @Override
        public boolean hasNext() {
            return currentDoc_ < documents_.size();
        }

        @Override
        public Document.NlpDocument next() {
            return documents_.get(currentDoc_++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator is read-only!");
        }
    }
}
