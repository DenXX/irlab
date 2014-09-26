package edu.emory.mathcs.clir.relextract.data;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Input provider, that reads Y!Answers Q&A pairs from WebScope XML format and
 * returns them as Stanford CoreNLP Annotation objects.
 */
public class YahooAnswersWebscopeXmlInputProvider extends InputProvider {
    private final XMLEventReader reader_;

    /**
     * Creates YahooAnswers WebScope dataset input provider, the filename should
     * be provided as the YahooAnswersWebscopeXmlInputProvider.InputPath
     * property.
     *
     * @param properties A set of properties, the input provider will read the
     *                   YahooAnswersWebscopeXmlInputProvider.InputPath
     *                   property for an input filename.
     * @throws FileNotFoundException
     * @throws XMLStreamException
     */
    public YahooAnswersWebscopeXmlInputProvider(Properties properties)
            throws FileNotFoundException, XMLStreamException {
        // TODO(denxx): Replace "input" with something better.
        String filename = properties.getProperty("input");
        XMLInputFactory factory = XMLInputFactory.newFactory();
        reader_ = factory.createXMLEventReader(
                new BufferedInputStream(new FileInputStream(filename)));
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
}
