package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.AppParameters;
import edu.stanford.nlp.util.Pair;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Input provider, that reads Y!Answers Q&A pairs from WebScope XML format and
 * returns them as Stanford CoreNLP Annotation objects.
 */
public class YahooAnswersWebscopeXmlInputProvider
        implements Iterable<Document.NlpDocument> {

    public YahooAnswersWebscopeXmlInputProvider(Properties props) {
        inputFilename_ = props.getProperty(AppParameters.INPUT_PARAMETER);
    }

    @Override
    public Iterator<Document.NlpDocument> iterator() {
        try {
            return new YahooAnswersWebscopeXmlInputIterator();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class YahooAnswersWebscopeXmlInputIterator
            implements Iterator<Document.NlpDocument> {
        private final XMLEventReader reader_;

        public YahooAnswersWebscopeXmlInputIterator()
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

        @Override
        public synchronized boolean hasNext() {
            return reader_.hasNext();
        }

        @Override
        public synchronized Document.NlpDocument next() {
            String question = "";
            String content = "";
            String answer = "";
            List<String> answers = new ArrayList<>();
            List<Pair<String, String>> attrs = new ArrayList<>();
            while (reader_.hasNext()) {
                try {
                    XMLEvent event = reader_.nextEvent();
                    if (event.isStartElement()) {
                        StartElement element = event.asStartElement();
                        switch (element.getName().toString()) {
                            case "subject":
                                question = getCurrentTagContent().trim().replaceAll("<(br|BR) ?/?>", "\n");
                                break;
                            case "content":
                                content = getCurrentTagContent().trim().replaceAll("<(br|BR) ?/?>", "\n");
                                break;
                            case "bestanswer":
                                answer = getCurrentTagContent().trim().replaceAll("<(br|BR) ?/?>", "\n");
                                break;
                            case "answer_item":
                                String txt = getCurrentTagContent().trim().replaceAll("<(br|BR) ?/?>", "\n");
                                if (!txt.equals(answer)) {
                                    answers.add(txt);
                                }
                                break;
                            case "nbestanswers":
                                break;
                            default:
                                attrs.add(new Pair<>(element.getName().toString(),
                                        getCurrentTagContent()));
                                break;
                        }
                    } else if (event.isEndElement() &&
                            event.asEndElement().getName().toString()
                                    .equals("vespaadd")) {
                        break;
                    }
                } catch (XMLStreamException e) {
                    throw new RuntimeException(e.getMessage());
                }
            }
            // Concatenating question and content, let's add question mark
            // if it is missing between.
            int titleLength = question.length();
            if (!question.equals(content)) {
                question += (question.length() > 0 && Character.isAlphabetic(question.charAt(question.length() - 1))
                        ? "? " : "  ") + content;
            }
            if (question.isEmpty()) return null;
            Document.NlpDocument.Builder docBuilder =
                    Document.NlpDocument.newBuilder();
            StringBuilder text = new StringBuilder();
            text.append(question);
            docBuilder.addPartsCharOffset(0);
            docBuilder.addPartsType(0);
            if (Character.isAlphabetic(question.charAt(question.length() - 1))) {
                text.append("?\n");
            } else {
                text.append("\n\n");
            }
            docBuilder.addPartsCharOffset(text.length());
            docBuilder.addPartsType(1);
            text.append(answer);

            for (String anotherAnswer : answers) {
                text.append(".\n");
                docBuilder.addPartsCharOffset(text.length());
                docBuilder.addPartsType(1);
                text.append(anotherAnswer);
            }

            docBuilder.setText(text.toString());
            docBuilder.setQuestionLength(question.length());
            docBuilder.setAnswerLength(answer.length());

            for (Pair<String, String> kv : attrs) {
                docBuilder.addAttributeBuilder().setKey(kv.first).setValue(kv.second);
            }
            return docBuilder.build();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator is read-only!");
        }
    }

    private final String inputFilename_;
}