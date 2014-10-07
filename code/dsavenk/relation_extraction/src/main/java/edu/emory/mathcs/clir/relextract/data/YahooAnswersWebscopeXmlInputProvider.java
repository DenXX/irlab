package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.AppParameters;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;

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
            String answer = "";
            HashMap<String, String> attrs = new HashMap<>();
            while (reader_.hasNext()) {
                try {
                    XMLEvent event = reader_.nextEvent();
                    if (event.isStartElement()) {
                        StartElement element = event.asStartElement();
                        switch (element.getName().toString()) {
                            case "subject":
                                question = getCurrentTagContent();
                                break;
                            case "bestanswer":
                                answer = getCurrentTagContent();
                                break;
                            default:
                                attrs.put(element.getName().toString(),
                                        getCurrentTagContent());
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
            if (question.isEmpty()) return null;
            Document.NlpDocument.Builder docBuilder =
                    Document.NlpDocument.newBuilder();
            docBuilder.setText(question + "\n" + answer);
            docBuilder.setQuestionLength(question.length());
            for (Map.Entry<String, String> kv : attrs.entrySet()) {
                docBuilder.addAttribute(Document.Attribute.newBuilder().setKey(
                        kv.getKey()).setValue(kv.getValue()));
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