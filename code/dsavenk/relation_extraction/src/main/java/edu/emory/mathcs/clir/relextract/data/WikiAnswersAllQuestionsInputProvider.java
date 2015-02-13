package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.AppParameters;

import javax.naming.OperationNotSupportedException;
import java.io.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 1/23/15.
 */
public class WikiAnswersAllQuestionsInputProvider implements Iterable<Document.NlpDocument> {

    private final String input_;

    public WikiAnswersAllQuestionsInputProvider(Properties props) {
        this.input_ = props.getProperty(AppParameters.INPUT_PARAMETER);
    }

    @Override
    public Iterator<Document.NlpDocument> iterator() {
        return new WikiAnswersIterator(input_);
    }

    public static class WikiAnswersIterator implements java.util.Iterator<Document.NlpDocument> {

        private final String basePath_;
        private int currentIndex_ = 0;
        private int counter_ = 0;
        private List<Document.NlpDocument> documents_ = new ArrayList<>();
        private int curDocumentIndex_ = 0;
        private BufferedReader inputReader_ = null;
        private List<Integer> offsets_ = new ArrayList<>();
        private List<Integer> types_ = new ArrayList<>();
        private StringBuilder text_ = new StringBuilder();

        public WikiAnswersIterator(String inputPath) {
            basePath_ = inputPath;
        }

        private String getNextFileName() {
            String index = Integer.toString(currentIndex_++);
            while (index.length() < 5) index = "0" + index;
            return basePath_ + index + ".gz";
        }

        private boolean readNext() {
            try {
                documents_.clear();
                List<String> questions = new ArrayList<>();
                List<String> answers = new ArrayList<>();
                while (answers.size() == 0 && questions.size() == 0) {
                    questions.clear();
                    answers.clear();
                    if (inputReader_ == null) {
                        String nextFileName = getNextFileName();
                        if (new File(nextFileName).exists()) {
                            inputReader_ = new BufferedReader(
                                    new InputStreamReader(
                                            new GZIPInputStream(
                                                    new FileInputStream(nextFileName))));
                        } else {
                            return false;
                        }
                    }

                    String linestr;
                    if ((linestr = inputReader_.readLine()) == null) {
                        inputReader_.close();
                        inputReader_ = null;
                    } else {
                        String[] line = linestr.split("\t");
                        for (String rec : line) {
                            String[] tp = rec.split(":");
                            try {
                                if (tp[0].equals("q")) {
                                    questions.add(tp[1]);
                                } else {
                                    answers.add(tp[1]);
                                }
                            } catch (ArrayIndexOutOfBoundsException exc) {
                                System.out.println(rec);
                                answers.clear();
                                break;
                            }
                        }
                    }
                }
                Document.NlpDocument.Builder doc = Document.NlpDocument.newBuilder();
                text_.setLength(0);
                offsets_.clear();
                for (String question : questions) {
                    if (text_.length() > 0) {
                        text_.append("?\n");
                    }
                    offsets_.add(text_.length());
                    types_.add(0);
                    text_.append(question);
                }
                int questionLength = text_.length();
                for (String answer : answers) {
                    if (text_.length() > 0) text_.append(".\n");
                    offsets_.add(text_.length());
                    types_.add(1);
                    text_.append(answer);
                }
                int answerLength = text_.length() - questionLength;
                doc.setText(text_.toString());
                doc.setQuestionLength(questionLength);
                doc.setAnswerLength(answerLength);
                for (int offset : offsets_) {
                    doc.addPartsCharOffset(offset);
                }
                for (int type : types_) {
                    doc.addPartsType(type);
                }
                doc.setDocId(Integer.toString(++counter_));
                documents_.add(doc.build());
                curDocumentIndex_ = 0;

            } catch (IOException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }

        @Override
        public boolean hasNext() {
            return curDocumentIndex_ < documents_.size() || readNext();
        }

        @Override
        public Document.NlpDocument next() {
            return documents_.get(curDocumentIndex_++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator is read-only!");
        }
    }
}
