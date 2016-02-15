package edu.emory.mathcs.ir.cqa_dialog;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dsavenk on 2/10/16.
 */
public class QuestionArchive {

    public static Question[] readQuestions(InputStream postsStream,
                                     InputStream commentsStream) throws IOException, ParserConfigurationException, SAXException {
        Map<Integer, Question> questions = new HashMap<>();

        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
        Document doc = dBuilder.parse(postsStream);
        doc.getDocumentElement().normalize();
        NodeList posts = doc.getElementsByTagName("row");
        for (int index = 0; index < posts.getLength(); ++index) {
            Element post = (Element)posts.item(index);
            if (Integer.parseInt(post.getAttribute("PostTypeId")) == 1) {
                // Question
                int id = Integer.parseInt(post.getAttribute("Id"));
                String title = post.getAttribute("Title");
                String body = post.getAttribute("Body");
                questions.put(id, new Question(id, title, body));
            } else {
                // Answer
                // Skip for now...
            }
        }

        dBuilder = dbFactory.newDocumentBuilder();
        doc = dBuilder.parse(commentsStream);
        doc.getDocumentElement().normalize();
        posts = doc.getElementsByTagName("row");
        for (int index = 0; index < posts.getLength(); ++index) {
            Element comment = (Element) posts.item(index);
            int questionId = Integer.parseInt(comment.getAttribute("PostId"));
            if (questions.containsKey(questionId)) {
                String commentText = comment.getAttribute("Text");
                questions.get(questionId).addComment(commentText);
            }
        }

        return questions.values().toArray(new Question[questions.size()]);
    }

}
