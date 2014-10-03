package edu.emory.mathcs.clir.relextract.utils;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;

import java.util.Properties;

/**
 * Provides an interface to access knowledge base, stored with Apache Jena.
 */
public class KnowledgeBase {

    /**
     * Property name to store the location of Apache Jena model of the KB.
     */
    public static final String KB_PROPERTY = "kb";

    /**
     * Prefix of Freebase RDF.
     */
    // TODO(denxx): Can we get this prefix from the KB?
    public static final String FREEBASE_RDF_PREFIX =
            "http://rdf.freebase.com/ns/";
    private static KnowledgeBase kb_ = null;
    private Model model_;

    /**
     * Private constructor, that initializes a new instance of the knowledge
     * base.
     *
     * @param location Location of the Apache Jena model of the KB.
     */
    private KnowledgeBase(String location) {
        Dataset dataset = TDBFactory.createDataset(location);
        dataset.begin(ReadWrite.READ);
        model_ = dataset.getDefaultModel();
    }

    /**
     * Factory method to return the only singleton instance of the knowledge
     * base.
     *
     * @param props Properties object, we will need location of the KB model
     *              from the 'kb' property.
     * @return The instance of knowledge base.
     */
    public static synchronized KnowledgeBase getInstance(Properties props) {
        if (kb_ == null) {
            kb_ = new KnowledgeBase(props.getProperty(KB_PROPERTY));
        }
        return kb_;
    }

    private String convertFreebaseMidRdf(String mid) {
        if (mid.startsWith("/")) mid = mid.substring(1);
        return FREEBASE_RDF_PREFIX + mid.replace("/", ".");
    }

    public StmtIterator getSubjectTriples(String subject) {
        return model_.listStatements(new SimpleSelector(
                model_.getResource(convertFreebaseMidRdf(subject)),
                null, (RDFNode) null));
    }

    /**
     * Returns iterator to all statements between the given subject and object.
     *
     * @param subject Subject entity id.
     * @param object  Object (maybe entity or literal).
     * @return Iterator to all the triples between the given arguments.
     */
    public StmtIterator getSubjectObjectTriples(String subject, String object) {
        return model_.listStatements(new SimpleSelector(
                model_.getResource(convertFreebaseMidRdf(subject)),
                null,
                model_.getResource(convertFreebaseMidRdf(object))));
    }

    public void getSubjectObjectTriplesCVT(String subject, String object) {
        StmtIterator iter = model_.listStatements(
                model_.getResource(convertFreebaseMidRdf("/m/01z0ks_")),
                null, (RDFNode) null);
        while (iter.hasNext()) {
            Statement triple = iter.nextStatement();
            System.out.println(triple);
        }
    }

    /**
     * Returns iterator to all statement between the given subject and object
     * literal, the type of the literal must be provided as the last argument of
     * the method.
     *
     * @param subject    Subject entity id.
     * @param object     Object literal as a string.
     * @param objectType Type of the object literal.
     * @return Iterator to statements between the given literals.
     */
    public StmtIterator getSubjectObjectTriples(String subject, String object,
                                                RDFDatatype objectType) {
        return model_.listStatements(new SimpleSelector(
                model_.getResource(convertFreebaseMidRdf(subject)),
                null,
                model_.createTypedLiteral(object, objectType)));
    }

    public StmtIterator getSubjectPredicateTriples(String subject,
                                                   String predicate) {
        return model_.listStatements(new SimpleSelector(
                model_.getResource(convertFreebaseMidRdf(subject)),
                model_.getProperty(FREEBASE_RDF_PREFIX, predicate),
                (RDFNode) null));
    }
}
