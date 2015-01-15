package edu.emory.mathcs.clir.relextract.tools;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDBFactory;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.stanford.nlp.classify.LinearClassifier;

import java.util.Properties;

/**
 * Created by dsavenk on 11/25/14.
 */
public class ModelExaminer {
    public static void main(String[] args) {
        Properties props = new Properties();
        props.setProperty("kb", args[0]);
        KnowledgeBase kb = KnowledgeBase.getInstance(props);
        System.out.println(kb.getPredicateDomainAndRange("people.person.date_of_birth"));
        boolean f = kb.isTripleTypeCompatible(new KnowledgeBase.Triple("/m/04cbtrw", "people.person.profession", "/m/0kyk"));
        boolean s = kb.isTripleTypeCompatible(new KnowledgeBase.Triple("/m/0254yc", "people.person.date_of_birth", "<DATE>"));
        System.out.println(f + " - " + s);
//        Dataset dataset = TDBFactory.createDataset(args[0]);
//        dataset.begin(ReadWrite.READ);
//        Model model = dataset.getDefaultModel();
//        StmtIterator iter = model.listStatements(model.getResource("http://rdf.freebase.com/ns/book.book.characters"),
//                null, (RDFNode) null);
//        while (iter.hasNext()) {
//            System.out.println(iter.nextStatement());
//        }
//        iter = model.listStatements(model.getResource("http://rdf.freebase.com/ns/book.book_character.appears_in_book"),
//                null, (RDFNode) null);
//        while (iter.hasNext()) {
//            System.out.println(iter.nextStatement());
//        }
//        iter = model.listStatements(model.getResource("http://rdf.freebase.com/ns/m.0gx91sz"),
//                model.getProperty("http://rdf.freebase.com/ns/type.object.type"), (RDFNode) null);
//        while (iter.hasNext())
//            System.out.println(iter.nextStatement().getObject().asResource().getLocalName());


        //LinearClassifier<String, Integer> model = LinearClassifier.readClassifier(args[0]);
        //System.out.println(model.topFeaturesToString(model.getTopFeatures(0.00001, true, 1000)));

    }
}
