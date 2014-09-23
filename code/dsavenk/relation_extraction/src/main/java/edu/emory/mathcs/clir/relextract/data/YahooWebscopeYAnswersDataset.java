package edu.emory.mathcs.clir.relextract.data;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by dsavenk on 9/22/14.
 */
public class YahooWebscopeYAnswersDataset implements
        Iterable<QuestionAnswerAnnotation> {

    public YahooWebscopeYAnswersDataset(InputStream stream)
            throws XMLStreamException {
        XMLInputFactory factory = XMLInputFactory.newFactory();
        reader_ = factory.createXMLEventReader(stream);
    }

    private XMLEventReader reader_;

    @Override
    public Iterator<QuestionAnswerAnnotation> iterator() {
        return new YahooWebscopeYAnswersDatasetIterator();
    }

    /**
     * Iterator class that traverses the list of Q&A pairs parsed. Iterator is
     * thread-safe, both hasNext and next methods are marked as synchronized.
     */
    class YahooWebscopeYAnswersDatasetIterator implements
            Iterator<QuestionAnswerAnnotation> {

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
        public synchronized QuestionAnswerAnnotation next() {
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
            QuestionAnswerAnnotation res =
                    new QuestionAnswerAnnotation(question, answer);
            for (Map.Entry<String, String> kv : attrs.entrySet()) {
                res.addAttribute(kv.getKey(), kv.getValue());
            }
            return res;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator is read-only.");
        }
    }
}
