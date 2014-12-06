package edu.emory.mathcs.clir.relextract.utils;

import com.hp.hpl.jena.datatypes.DatatypeFormatException;
import com.hp.hpl.jena.datatypes.RDFDatatype;
import com.hp.hpl.jena.datatypes.xsd.XSDDatatype;
import com.hp.hpl.jena.query.*;
import com.hp.hpl.jena.rdf.model.*;
import com.hp.hpl.jena.tdb.TDBFactory;
import edu.stanford.nlp.time.Timex;
import edu.stanford.nlp.util.Pair;
import org.apache.commons.collections4.map.LRUMap;
import com.hp.hpl.jena.datatypes.xsd.XSDDateTime;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Provides an interface to access knowledge base, stored with Apache Jena.
 */
public class KnowledgeBase {

    /**
     * Property name to store the location of Apache Jena model of the KB.
     */
    public static final String KB_PROPERTY = "kb";

    public static final String CVT_PREDICATE_LIST_PARAMETER = "cvt";

    /**
     * Prefix of Freebase RDF.
     */
    // TODO(denxx): Can we get this prefix from the KB?
    public static final String FREEBASE_RDF_PREFIX =
            "http://rdf.freebase.com/ns/";
    private static KnowledgeBase kb_ = null;
    private final Set<String> cvtProperties_ = new HashSet<>();
    private final Set<String> dateProperties = new HashSet<>();
    public Model model_;
    private Map<Pair<String, String>, List<Triple>> tripleCache_ =
            Collections.synchronizedMap(new LRUMap<>(1000000));

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
                        "freebase.type_hints.mediator"), (RDFNode) null);
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
                            (RDFNode) null);
                    while (iter3.hasNext()) {
                        cvtProperties_.add(iter3.nextStatement().getObject().asLiteral().toString());
                    }
                }
            }
        }

        iter = model_.listStatements(null,
                model_.getProperty(FREEBASE_RDF_PREFIX, "type.property.expected_type"),
                model_.getResource(convertFreebaseMidRdf("type.datetime")));
        while (iter.hasNext()) {
            dateProperties.add(iter.nextStatement().getSubject().getURI());
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

    private String getEntityName(String mid) {
        StmtIterator iter = model_.listStatements(
                model_.getResource(convertFreebaseMidRdf(mid)),
                model_.getProperty(FREEBASE_RDF_PREFIX,
                        "type.object.name"), (RDFNode) null);
        while (iter.hasNext()) {
            Statement st = iter.nextStatement();
            if (st.getObject().asLiteral().getLanguage().equals("en")) {
                return st.getString();
            }
        }
        return "";
    }

    public long getTripleCount(String mid) {
        long count = 0;
        StmtIterator iter = model_.getResource(convertFreebaseMidRdf(mid)).listProperties();
        while (iter.hasNext()) {
            iter.nextStatement();
            ++count;
        }
        return count;
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

    /**
     * Returns iterator to all statements between the given subject and object.
     *
     * @param subject Subject entity id.
     * @param object  Object (maybe entity or literal).
     * @return Iterator to all the triples between the given arguments.
     */
    public List<Triple> getSubjectObjectTriples(String subject, String object, List<Pair<String, String>> cvtPredicates) {
        Pair<String, String> so = new Pair<>(subject, object);
        if (tripleCache_.containsKey(so)) {
            return tripleCache_.get(so);
        }
        List<Triple> res = null;
        StmtIterator iter = getSubjectObjectTriples(subject, object);
        while (iter.hasNext()) {
            if (res == null) res = new ArrayList<>();
            res.add(new Triple(iter.nextStatement()));
        }

        if (cvtPredicates != null) {
            for (Pair<String, String> cvtPred : cvtPredicates) {
                iter = getSubjectPredicateTriples(subject, cvtPred.first);
                while (iter.hasNext()) {
                    Statement t = iter.nextStatement();
                    if (t.getObject().isResource()) {
                        StmtIterator iter2 = getSubjectObjectTriples(t.getObject().asResource().getLocalName(), object);
                        while (iter2.hasNext()) {
                            Statement t2 = iter2.nextStatement();
                            if (res == null) res = new ArrayList<>();
                            res.add(new Triple(subject, t.getPredicate().getLocalName() + "|" + t2.getPredicate().getLocalName(), object));
                        }
                    }
                }
            }
        }
        tripleCache_.put(so, res);
        return res;
    }

    /**
     * Constructs and runs a SPARQL query to get all paths between 2 entities
     * that run through an intermediate CVT node. Unfortunately the method is
     * slow and it is faster just to enumerate neighbours manually
     * (see {@link #getSubjectObjectTriplesCVT(String, String)}).
     *
     * @param subject Id of the object entity.
     * @param object  Id of the subject entity.
     * @return A set of
     * {@link edu.emory.mathcs.clir.relextract.utils.KnowledgeBase.Triple}
     * objects.
     */
    public Set<Triple> getSubjectObjectTriplesCVTSparql(String subject,
                                                        String object) {
        String queryString = String.format(
                "PREFIX xsd: <http://www.w3.org/2001/XMLSchema#>" +
                        "PREFIX fb: <http://rdf.freebase.com/ns/>" +
                        "SELECT ?pred1 ?pred2 { " +
                        "   fb:%s ?pred1 ?cvt . " +
                        "   BIND(CONCAT(\"/\", REPLACE(STRAFTER(xsd:string(?pred1)," +
                        "\"http://rdf.freebase.com/ns/\"), \"\\\\.\", \"/\")) AS ?pred1NameFixed) " +
                        "   ?predId fb:type.object.id ?pred1NameFixed ; " +
                        "           fb:type.property.expected_type ?mediatorType . " +
                        "   ?mediatorType fb:freebase.type_hints.mediator true . " +
                        "   ?cvt ?pred2 fb:%s . " +
                        "} ",
                subject.replace("/", ".").substring(1),
                object.replace("/", ".").substring(1));

        Query query = QueryFactory.create(queryString);
        QueryExecution qexec = QueryExecutionFactory.create(query, model_);
        ResultSet results = qexec.execSelect();
        Set<Triple> res = new HashSet<>();
        while (results.hasNext()) {
            QuerySolution soln = results.nextSolution();
            String pred1 = soln.get("pred1").toString();
            pred1 = pred1.substring(pred1.lastIndexOf("/")).replace(".", "/");
            String pred2 = soln.get("pred2").toString();
            pred2 = pred2.substring(pred2.lastIndexOf("/")).replace(".", "/");
            res.add(new Triple(subject, pred1 + "." + pred2, object));
        }
        return res;
    }

    /**
     * Returns a set of triples, that connects a pair of entities, including
     * thouse paths that go through a CVT node.
     *
     * @param subject Id of the object entity.
     * @param object  Id of the subject entity.
     * @return A set of
     * {@link edu.emory.mathcs.clir.relextract.utils.KnowledgeBase.Triple}
     * objects.
     */
    public Set<Triple> getSubjectObjectTriplesCVT(String subject, String object) {
        final String subjectUri = convertFreebaseMidRdf(subject);
        final String objectUri = convertFreebaseMidRdf(object);
        Set<Triple> res = new HashSet<>();
        StmtIterator iter = model_.listStatements(
                model_.getResource(subjectUri), null, (RDFNode) null);
        while (iter.hasNext()) {
            Statement triple = iter.nextStatement();
            if (isCVTProperty(triple.getPredicate().getLocalName()) &&
                    triple.getObject().isResource()) {
                StmtIterator cvtPropsIterator = model_.listStatements(
                        triple.getObject().asResource(), null,
                        model_.getResource(objectUri));
                while (cvtPropsIterator.hasNext()) {
                    Statement cvtTriple = cvtPropsIterator.nextStatement();
                    Triple cvtTripleRes = new Triple(cvtTriple);
                    cvtTripleRes.subject = subject;
                    cvtTripleRes.predicate = "/" +
                            triple.getPredicate().getLocalName()
                                    .replace(".", "/") +
                            "./" + cvtTriple.getPredicate().getLocalName()
                            .replace(".", "/");
                    res.add(cvtTripleRes);
                }
            } else {
                if (triple.getObject().isResource() &&
                        triple.getObject().asResource().getURI()
                                .equals(objectUri)) {
                    Triple t = new Triple(triple);
                    t.predicate = "/" + t.predicate.replace(".", "/");
                    res.add(t);
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
     * @param subject     Subject entity id.
     * @param measure     Object measure as a String.
     * @param measureType Type of the object literal.
     * @return Iterator to statements between the given literals.
     */
    public List<Triple> getSubjectMeasureTriples(String subject, String measure,
                                                 String measureType) {
        RDFDatatype objectType = XSDDatatype.XSDstring;
        try {
            Timex tm;
            Matcher matcher;
            switch (measureType) {
                case "TIME":
                    tm = Timex.fromXml(measure);
                    measure = tm.value() != null ? tm.value() : tm.altVal();
                    objectType = XSDDatatype.XSDdateTime;
                    break;
                case "DATE":
                    tm = Timex.fromXml(measure);
                    measure = tm.value() != null ? tm.value() : tm.altVal();
                    objectType = XSDDatatype.XSDdate;
                    if (measure.matches("[X0-9]{4}(-[X0-9]{2}){0,2}")) {
                        String year = measure.substring(0, 4);
                        boolean fullYear = !year.contains("X");
                        String month = measure.length() > 4 ?
                                measure.substring(5, 7)
                                : "XX";
                        boolean fullMonth = !month.contains("X");
                        String day = measure.length() > 7
                                ? measure.substring(8)
                                : "XX";
                        boolean fullDay = !day.contains("X");
                        if (fullYear && fullMonth && fullDay) {
                            objectType = XSDDatatype.XSDdate;
                        } else if (fullYear && fullMonth) {
                            objectType = XSDDatatype.XSDgYearMonth;
                        } else if (fullYear) {
                            objectType = XSDDatatype.XSDgYear;
                        } else if (fullMonth && fullDay) {
                            objectType = XSDDatatype.XSDgMonthDay;
                        } else if (fullMonth) {
                            objectType = XSDDatatype.XSDgMonth;
                        }
                    } else {
                        return null;
                    }
                    break;
                case "DURATION":
                    // Duration is in a TimeML format and we will just extract
                    // the number.
                    tm = Timex.fromXml(measure);
                    String val = tm.value() != null ? tm.value() : tm.altVal();
                    matcher = Pattern.compile("\\d+").matcher(val);
                    matcher.find();
                    measure = matcher.group();
                    objectType = XSDDatatype.XSDfloat;
                    break;
                case "MONEY":
                case "PERCENT":
                    matcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+")
                            .matcher(measure);
                    matcher.find();
                    measure = String.format("%.2f", Float.parseFloat(matcher.group()));
                    objectType = XSDDatatype.XSDfloat;
                    break;
                case "NUMBER":
                    if (measure.indexOf(".") != -1) {
                        matcher = Pattern.compile("[-+]?[0-9]*\\.?[0-9]+")
                                .matcher(measure);
                        matcher.find();
                        measure = String.format("%.2f", Float.parseFloat(matcher.group()));
                        objectType = XSDDatatype.XSDfloat;
                    } else
                        objectType = XSDDatatype.XSDinteger;
                    break;
                case "ORDINAL":
                    objectType = XSDDatatype.XSDinteger;
                    break;
                default:
                    break;
            }
        } catch (Exception exc) {
            // Something went wrong, still try just with a string.
            objectType = XSDDatatype.XSDstring;
        }
        StmtIterator iter = model_.listStatements(new SimpleSelector(
                model_.getResource(convertFreebaseMidRdf(subject)),
                null,
                model_.createTypedLiteral(measure, objectType)));
        List<Triple> res = null;
        while (iter.hasNext()) {
            if (res == null) res = new ArrayList<>();
            res.add(new Triple(iter.nextStatement()));
        }
        return res;
    }


    public List<Triple> getSubjectDateSoftMatchTriples(String subject, String dateMeasure) {
        Timex tm;
        String measureStr;
        try {
            tm = Timex.fromXml(dateMeasure);
            measureStr = tm.value() != null ? tm.value() : tm.altVal();
        } catch (Exception e) {
            measureStr = dateMeasure;
        }
        // Full match
        if (measureStr.matches("[X0-9]{4}(-[X0-9]{2}){0,2}")) {
            String year = measureStr.substring(0, 4);
            boolean fullYear = !year.contains("X");
            String month = measureStr.length() > 4 ?
                    measureStr.substring(5, 7)
                    : "XX";
            boolean fullMonth = !month.contains("X");
            String day = measureStr.length() > 7
                    ? measureStr.substring(8)
                    : "XX";
            boolean fullDay = !day.contains("X");
            if (fullDay && fullMonth && fullYear) {
                return getSubjectMeasureTriples(subject, dateMeasure, "DATE");
            } else {
                List<Triple> res = new ArrayList<>();
                for (String datePredicate : dateProperties) {
                    StmtIterator iter = model_.listStatements(model_.getResource(convertFreebaseMidRdf(subject)),
                            model_.getProperty(datePredicate), (RDFNode)null);
                    if (iter.hasNext()) {
                        Statement triple = iter.nextStatement();
                        try {
                            XSDDateTime value = (XSDDateTime) triple.getObject().asLiteral().getValue();
                            boolean valueHasYear = (value.getNarrowedDatatype() == XSDDatatype.XSDdate)
                                    || (value.getNarrowedDatatype() == XSDDatatype.XSDgYearMonth)
                                    || (value.getNarrowedDatatype() == XSDDatatype.XSDgYear);
                            boolean valueHasMonth = (value.getNarrowedDatatype() == XSDDatatype.XSDdate)
                                    || (value.getNarrowedDatatype() == XSDDatatype.XSDgYearMonth)
                                    || (value.getNarrowedDatatype() == XSDDatatype.XSDgMonthDay)
                                    || (value.getNarrowedDatatype() == XSDDatatype.XSDgMonth);
                            boolean valueHasDay = (value.getNarrowedDatatype() == XSDDatatype.XSDdate)
                                    || (value.getNarrowedDatatype() == XSDDatatype.XSDgMonthDay)
                                    || (value.getNarrowedDatatype() == XSDDatatype.XSDgDay);
                            boolean eq = true;
                            if (valueHasYear) {
                                String valYearStr = Integer.toString(value.getYears());
                                eq &= compareDatePartsSoft(year, valYearStr);
                                eq &= year.charAt(2) != 'X' && year.charAt(3) != 'X';
                            }
                            if (valueHasMonth) {
                                String valMonthStr = Integer.toString(value.getMonths());
                                eq &= compareDatePartsSoft(month, valMonthStr);
                            }
                            if (valueHasDay) {
                                String valDayStr = Integer.toString(value.getDays());
                                eq &= compareDatePartsSoft(month, valDayStr);
                            }
                            if (eq) {
                                res.add(new Triple(triple));
                            }
                        } catch (DatatypeFormatException exc) {} // Skipping format exceptions.
                    }
                }
            }
        }
        return null;
    }

    private boolean compareDatePartsSoft(String value1, String value2) {
        if (value1.length() != value2.length()) {
            return false;
        } else {
            for (int i = 0; i < value1.length(); ++i) {
                if (value1.charAt(i) != 'X' && value1.charAt(i) != value2.charAt(i)) {
                    return false;
                }
            }
        }
        return true;
    }

    public StmtIterator getSubjectPredicateTriples(String subject,
                                                   String predicate) {
        return model_.listStatements(model_.getResource(convertFreebaseMidRdf(subject)),
                model_.getProperty(FREEBASE_RDF_PREFIX, predicate),
                (RDFNode) null);
    }

    public boolean isCVTProperty(String property) {
        String prop = property.startsWith("/") ? property : "/" + property;
        return cvtProperties_.contains(prop.replace(".", "/"));
    }

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
            return this.compareTo((Triple) triple) == 0;
        }

        @Override
        public String toString() {
            // TODO(denxx): Can't access private member. It might not exist and
            // it is bad anyway.
            return kb_.getEntityName(subject) + " [" + subject + "] - "
                    + predicate + " - " + kb_.getEntityName(object) +
                    " [" + object + "]";
        }
    }
}
