package edu.emory.mathcs.clir.relextract.utils;

import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.RDFNode;
import com.hp.hpl.jena.rdf.model.SimpleSelector;
import com.hp.hpl.jena.rdf.model.StmtIterator;
import com.hp.hpl.jena.tdb.TDBFactory;

/**
 * Provides an interface to access knowledge base, stored with Apache Jena.
 */
public class KnowledgeBase {

    public static final String FREEBASE_RDF_PREFIX =
            "http://rdf.freebase.com/ns/";

    public KnowledgeBase(String location) {
        Dataset dataset = TDBFactory.createDataset(location);
        dataset.begin(ReadWrite.READ);
        model_ = dataset.getDefaultModel();
    }

    public StmtIterator getSubjectTriples(String subject) {
        return model_.listStatements(new SimpleSelector(
                model_.getResource(FREEBASE_RDF_PREFIX +
                        subject.replace("/", ".")),
                null, (RDFNode)null));
    }

    private Model model_;
}
