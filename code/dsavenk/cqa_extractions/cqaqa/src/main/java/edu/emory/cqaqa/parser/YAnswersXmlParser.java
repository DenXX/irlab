package edu.emory.cqaqa.parser;

import edu.emory.cqaqa.processor.QuestionAnswerPairProcessor;
import edu.emory.cqaqa.types.QuestionAnswerPair;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;

/**
 * Static class that parses Yahoo Answers WebScope XML format.
 */
public class YAnswersXmlParser {

    /**
     * Parses Yahoo Answers XML file. For each qa processor.process method is called.
     * @param fileName XML filename.
     * @param processor Processor to call for each qa.
     */
    public static void parse(String fileName, final QuestionAnswerPairProcessor processor)
            throws IOException,SAXException, ParserConfigurationException {

        SAXParserFactory factory = SAXParserFactory.newInstance();
        SAXParser saxParser = factory.newSAXParser();

        DefaultHandler handler = new DefaultHandler() {
            boolean qid = false;
            boolean question = false;
            boolean content = false;
            boolean bestanswer = false;
            boolean category = false;
            boolean mainCategory = false;
            boolean language = false;
            StringBuilder text = new StringBuilder();
            QuestionAnswerPair qa = null;

            public void startElement(String uri, String localName, String qName,
                                     Attributes attributes) throws SAXException {
                if (qName.equalsIgnoreCase("uri")) {
                    qid = true;
                    if (qa != null)
                        processor.processPair(qa);
                    qa = new QuestionAnswerPair();
                }
                else if (qName.equalsIgnoreCase("subject")) {
                    question = true;
                } else if (qName.equalsIgnoreCase("content")) {
                    content = true;
                } else if (qName.equalsIgnoreCase("bestanswer")) {
                    bestanswer = true;
                } else if (qName.equalsIgnoreCase("cat")) {
                    category = true;
                } else if (qName.equalsIgnoreCase("maincat")) {
                    mainCategory = true;
                } else if (qName.equalsIgnoreCase("language")) {
                    language = true;
                }
                text = new StringBuilder();
            }

            public void endElement(String uri, String localName,
                                   String qName) throws SAXException {
                if (qName.equalsIgnoreCase("uri")) {
                    qid = false;
                    qa.addAttribute("id", text.toString());
                } else if (qName.equalsIgnoreCase("subject")) {
                    question = false;
                    qa.setQuestion(text.toString());
                } else if (qName.equalsIgnoreCase("content")) {
                    content = false;
                    qa.addAttribute("content", text.toString());
                } else if (qName.equalsIgnoreCase("bestanswer")) {
                    bestanswer = false;
                    qa.setAnswer(text.toString());
                } else if (qName.equalsIgnoreCase("maincat")) {
                    mainCategory = false;
                    qa.addAttribute("maincat", text.toString());
                } else if (qName.equalsIgnoreCase("cat")) {
                    category = false;
                    qa.addAttribute("cat", text.toString());
                } else if (qName.equalsIgnoreCase("language")) {
                    language = false;
                    qa.addAttribute("language", text.toString());
                }
            }

            public void characters(char ch[], int start, int length) throws SAXException {
                if (qid || question || content || bestanswer || category || mainCategory || language)
                    text.append(new String(ch, start, length));
            }
        };
        saxParser.parse(fileName, handler);
    }
}