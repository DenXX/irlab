package edu.emory.mathcs.ir.input;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;

/**
 * Input provider, that reads Y!Answers Q&A pairs from WebScope XML format and
 * returns them as Stanford CoreNLP Annotation objects.
 */
public class YahooAnswersXmlInput
        implements Iterable<YahooAnswersXmlInput.QnAPair> {

    /**
     * The substring that can be found inside the dummy answer saying that
     * the question period has expired.
     */
    public static final String EXPIRED_QUESTION_MARKER =
            "question period has expired";

    /**
     * Represents a QnA pair read from Yahoo! Answers XML data.
     */
    public static class QnAPair {
        public final String id;
        public final String questionTitle;
        public final String questionBody;
        public final String[] categories;
        public final String bestAnswer;
        public final Map<String, String> attributes;

        /**
         * Creates a new QnA pair with the given values.
         * @param id Unique Id of the QnA pair.
         * @param questionTitle The title of the question.
         * @param questionBody The body of the question.
         * @param mainCategory The main category of the question.
         * @param subcategory The subcategory of the question.
         * @param bestAnswer The best answer to the question if specified or
         *                   the first answer in the list.
         */
        public QnAPair(String id, String questionTitle, String questionBody,
                       String[] categories,  String bestAnswer,
                       Map<String, String> attributes) {
            this.id = id;
            this.questionTitle = questionTitle;
            this.questionBody = questionBody;
            this.categories = categories;
            this.bestAnswer = bestAnswer;
            this.attributes = attributes;
        }
    }

    private final String inputFilename_;
    private int counter_ = 0;

    /**
     * Creates an iterable Yahoo! Answers Webscope XML input to read the data
     * from the given file.
     * @param inputFile Full path to the xml file with question-answer data.
     */
    public YahooAnswersXmlInput(String inputFile) {
        inputFilename_ = inputFile;
    }

    @Override
    public Iterator<QnAPair> iterator() {
        try {
            return new YahooAnswersXmlInputIterator();
        } catch (FileNotFoundException | XMLStreamException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class YahooAnswersXmlInputIterator implements Iterator<QnAPair> {
        private final XMLEventReader reader_;

        public YahooAnswersXmlInputIterator()
                throws FileNotFoundException, XMLStreamException {
            XMLInputFactory factory = XMLInputFactory.newFactory();
            reader_ = factory.createXMLEventReader(
                    new BufferedInputStream(
                            new FileInputStream(inputFilename_)));
        }

        private String getCurrentTagContent() throws XMLStreamException {
            if (reader_.hasNext()) {
                XMLEvent event = reader_.nextEvent();
                StringBuilder res = new StringBuilder();
                while (reader_.hasNext() && event.isCharacters()) {
                    res.append(event.asCharacters().getData());
                    event = reader_.nextEvent();
                }
                return res.toString();
            }
            return null;
        }

        private String getNormalizedTagContent() throws XMLStreamException {
            return getCurrentTagContent().trim().replaceAll(
                    "<(br|BR) ?/?>", "\n");
        }

        @Override
        public synchronized boolean hasNext() {
            return reader_.hasNext();
        }

        @Override
        public synchronized QnAPair next() {
            String id = "";
            String questionTitle = "";
            String questionBody = "";
            String bestAnswer = "";
            String mainCategory = "";
            String subcategory = "";
            String category = "";
            List<String> answers = new ArrayList<>();
            Map<String, String> attributes = new HashMap<>();
            boolean insideDocument = false;
            while (reader_.hasNext()) {
                try {
                    XMLEvent event = reader_.nextEvent();
                    if (event.isStartElement()) {
                        StartElement element = event.asStartElement();
                        switch (element.getName().toString()) {
                            case "document":
                                insideDocument = true;
                                break;
                            case "subject":
                                questionTitle = getNormalizedTagContent();
                                break;
                            case "content":
                                questionBody = getNormalizedTagContent();
                                break;
                            case "bestanswer":
                                bestAnswer = getNormalizedTagContent();
                                break;
                            case "answer_item":
                                String txt = getNormalizedTagContent();
                                if (!txt.equals(bestAnswer)) {
                                    answers.add(txt);
                                }
                                break;
                            case "maincat":
                                mainCategory = getNormalizedTagContent();
                                break;
                            case "subcat":
                                subcategory = getNormalizedTagContent();
                                break;
                            case "cat":
                                category = getNormalizedTagContent();
                                break;
                            case "uri":
                                id = getNormalizedTagContent();
                                break;
                            case "nbestanswers":
                                break;
                            default:
                                if (insideDocument) {
                                    attributes.put(element.getName().toString(),
                                            getNormalizedTagContent());
                                }
                                break;
                        }
                    } else if (event.isEndElement() &&
                            event.asEndElement().getName().toString()
                                    .equals("vespaadd")) {
                        break;
                    } else if (event.isEndElement() &&
                            event.asEndElement().getName().toString().equals(
                                    "document")) {
                        insideDocument = false;
                    }
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }

            if (bestAnswer.contains(EXPIRED_QUESTION_MARKER)) {
                bestAnswer = "";
            }

            // Check if the best answer is empty or it is a dummy answer saying
            // the question has expired.
            if (bestAnswer.isEmpty() && !answers.isEmpty()) {
                Optional<String> answer =
                        answers.stream().filter(
                                a -> !a.contains(EXPIRED_QUESTION_MARKER))
                                        .findFirst();
                if (answer.isPresent()) {
                    bestAnswer = answer.get();
                }
            }
            String[] categories;
            if (category.equals(subcategory)) {
                categories = new String[2];
                categories[0] = mainCategory;
                categories[1] = subcategory;
            } else {
                categories = new String[3];
                categories[0] = mainCategory;
                categories[1] = subcategory;
                categories[2] = category;
            }
            return new QnAPair(id, questionTitle, questionBody, categories,
                    bestAnswer, attributes);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator is read-only!");
        }
    }
}