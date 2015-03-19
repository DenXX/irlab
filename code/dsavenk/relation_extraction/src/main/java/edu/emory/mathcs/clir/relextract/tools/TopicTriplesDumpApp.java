package edu.emory.mathcs.clir.relextract.tools;

import com.hp.hpl.jena.rdf.model.Resource;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.impl.PropertyImpl;
import com.hp.hpl.jena.rdf.model.impl.ResourceImpl;
import com.hp.hpl.jena.rdf.model.impl.StatementImpl;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.io.*;
import java.util.Properties;

/**
 * Created by dsavenk on 3/19/15.
 */
public class TopicTriplesDumpApp {

    public static void main(String[] args) throws IOException {
        PrintWriter out = null;
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(new FileInputStream(args[0])));
            out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(args[2])));
            Properties props = new Properties();
            props.setProperty("kb", args[1]);
            KnowledgeBase kb = KnowledgeBase.getInstance(props);
            String mid;
            int counter = 0;
            while ((mid = input.readLine()) != null) {
                for (Statement st : kb.getSubjectTriplesCvt(mid)) {
                    out.println(String.format("%s\t%s\t%s", st.getSubject(), st.getPredicate(), st.getObject().asNode().toString(null, true).replace("\n", " ").replace("\t", " ")));
                }
                if (++counter % 100 == 0) System.out.println(counter);
            }
            input.close();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
