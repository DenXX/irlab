package edu.emory.mathcs.clir.relextract.utils;

import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.query.Dataset;
import com.hp.hpl.jena.query.ReadWrite;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;

import java.util.*;

/**
 * Provides an interface to access knowledge base, stored with Apache Jena.
 */
public class KnowledgeBase {

    public static class Triple implements Comparable<Triple> {
        public String subject;
        public String predicate;
        public String object;

        public Triple(Statement triple) {
            subject = "/" + triple.getSubject().getLocalName().replace(".", "/");
            predicate = triple.getPredicate().getLocalName();
            if (triple.getObject().isResource()) {
                object = "/" + triple.getObject().asResource().getLocalName().replace(".", "/");
            } else {
                object = triple.getObject().asLiteral().getString();
            }
        }

        public Triple(String subj, String pred, String obj) {
            subject = subj;
            predicate = pred;
            object = obj;
        }

        @Override
        public int compareTo(Triple triple) {
            return (subject + predicate + object).compareTo(
                    triple.subject + triple.predicate + triple.object);
        }

        @Override
        public int hashCode() {
            return (subject + predicate + object).hashCode();
        }

        @Override
        public boolean equals(Object triple) {
            if (!(triple instanceof Triple)) return false;
            if (this == triple) return true;
            return this.compareTo((Triple)triple) == 0;
        }
    }

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
        // Load all CVT properties.
        StmtIterator iter = model_.listStatements(null,
                model_.getProperty(FREEBASE_RDF_PREFIX,
                        "freebase.type_hints.mediator"), (RDFNode)null);
        while (iter.hasNext()) {
            Statement triple = iter.nextStatement();
            if (triple.getObject().asLiteral().getBoolean()) {
                StmtIterator iter2 = model_.listStatements(null,
                        model_.getProperty(FREEBASE_RDF_PREFIX,
                                "type.property.expected_type"),
                        triple.getSubject());
                while (iter2.hasNext()) {
                    Statement triple2 = iter2.nextStatement();
                    StmtIterator iter3 = model_.listStatements(triple2.getSubject(),
                            model_.getProperty(FREEBASE_RDF_PREFIX, "type.object.id"),
                            (RDFNode)null);
                    while (iter3.hasNext()) {
                        cvtProperties.add(iter3.nextStatement().getObject().asLiteral().toString());
                    }
                }
            }
        }
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
        return model_.listStatements(model_.getResource(convertFreebaseMidRdf(subject)),
                null,
                model_.getResource(convertFreebaseMidRdf(object)));
    }

    public Set<Triple> getSubjectObjectTriplesCVT(String subject, String object) {
        final String subjectUri = convertFreebaseMidRdf(subject);
        final String objectUri = convertFreebaseMidRdf(object);
        Set<Triple> res = new HashSet<>();
        StmtIterator iter = model_.listStatements(
                model_.getResource(subjectUri), null, (RDFNode)null);
        while (iter.hasNext()) {
            Statement triple = iter.nextStatement();
            if (isCVTProperty(triple.getPredicate().getLocalName()) && triple.getObject().isResource()) {
                StmtIterator cvtPropsIterator = model_.listStatements(
                        triple.getObject().asResource(), null, (RDFNode)null);
                while (cvtPropsIterator.hasNext()) {
                    Statement cvtTriple = cvtPropsIterator.nextStatement();
                    if (cvtTriple.getObject().isResource() &&
                            cvtTriple.getObject().asResource().getURI()
                                    .equals(objectUri)) {
                        Triple cvtTripleRes = new Triple(cvtTriple);
                        cvtTripleRes.subject = subject;
                        cvtTripleRes.predicate = "/" + triple.getPredicate().getLocalName().replace(".", "/") +
                                "./" + cvtTriple.getPredicate().getLocalName().replace(".", "/");
                        res.add(cvtTripleRes);
                    }
                }
            } else {
                if (triple.getObject().isResource() &&
                        triple.getObject().asResource().getURI()
                                .equals(objectUri)) {
                    res.add(new Triple(triple));
                }

            }
        }
        return res;
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

    public boolean isCVTProperty(String property) {
        String prop = property.startsWith("/") ? property : "/" + property;
        return cvtProperties.contains(prop.replace(".", "/"));
    }

    private final Set<String> cvtProperties = new HashSet<>();
}
