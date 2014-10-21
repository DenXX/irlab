package edu.emory.mathcs.clir.relextract.utils;

import java.util.Properties;

public class KnowledgeBaseTest {

    private KnowledgeBase kb_;

    @org.junit.Before
    public void setUp() throws Exception {
        // TODO(denxx): Create small model and put it to the resources.
        Properties props = new Properties();
        props.setProperty("kb", "/home/dsavenk/Projects/octiron/data/Freebase/" +
                "jena_model/");
        kb_ = KnowledgeBase.getInstance(props);
    }

//    @org.junit.Test
//    public void testGetSubjectTriples() throws Exception {
//        StmtIterator iter = kb_.getSubjectPredicateTriples("/m/053w4",
//                "people.person.date_of_birth");
//        assertTrue(iter.hasNext());
//        while (iter.hasNext()) {
//            Statement st = iter.nextStatement();
//            System.out.println(st.getObject().toString());
//            System.out.println(st.getObject().isLiteral());
//            System.out.println(st.getObject().asLiteral().getDatatype());
//            System.out.println(st.getObject().asLiteral().getString());
//        }
//    }
//
//    @org.junit.Test
//    public void testGetSubjectMeasureTriples() throws Exception {
//        EntityAnnotationProcessor processor = new EntityAnnotationProcessor(
//                new Properties());
//        processor.freeze();
//        Document.NlpDocument doc = Document.NlpDocument.newBuilder().setText(
//                "Barack Obama is 44th president of the USA and his passion is about 34.734 deep.").build();
//        doc = processor.process(doc);
//
//        StmtIterator iter = kb_.getSubjectMeasureTriples("/m/02mjmr", doc.getSpan(1).getValue(), doc.getSpan(3).getType());
//        Assert.assertTrue(iter.hasNext());
//        while (iter.hasNext()) {
//            Statement st = iter.nextStatement();
//            System.out.println(st.toString());
//        }
//    }
//
//    @Test
//    public void testSPARQL() throws Exception {
//        Set<KnowledgeBase.Triple> res =
//                kb_.getSubjectObjectTriplesCVTSparql("/m/02mjmr", "/m/025s5v9");
//        Assert.assertTrue(!res.isEmpty());
//        for (KnowledgeBase.Triple triple : res) {
//            System.out.println(triple.predicate);
//        }
//    }
//
//
//    @org.junit.Test
//    public void testGetSubjectObjectTriples() throws Exception {
//
//    }

//    @org.junit.Test
//    public void testGetSubjectTriples2() throws Exception {
//        Set<KnowledgeBase.Triple> triples =
//                kb_.getSubjectObjectTriplesCVT("/m/09b6zr", "/m/04g8d");
//        Assert.assertTrue(triples.isEmpty());
//    }

//    @Test
//    public void testSpellCheck() throws IOException, ParseException {
//        Directory spellIndexDir = FSDirectory.open(
//                new File("/home/dsavenk/Projects/octiron/data/Freebase/spellcheck_index/"));
//        Directory searchIndexDir = FSDirectory.open(
//                new File("/home/dsavenk/Projects/octiron/data/Freebase/lexicon_index/"));
//        IndexReader searchIndexReader = DirectoryReader.open(searchIndexDir);
//        SpellChecker spellChecker = new SpellChecker(spellIndexDir);
//        IndexSearcher searcher = new IndexSearcher(searchIndexReader);
//
//        Analyzer analyzer = new StandardAnalyzer(new CharArraySet(0, true));
//        QueryParser parser = new QueryParser("name", analyzer);
//
//        String name = "Asia";
//        Query query = parser.parse(name);
//        Set<Term> terms = new HashSet<>();
//        query.extractTerms(terms);
//            ScoreDoc[] docs = searcher.search(query, 10).scoreDocs;
//            for (ScoreDoc doc : docs) {
//                if (doc.score / docs[0].score < 0.8) break;
//                System.out.println(searcher.doc(doc.doc).get("name") + " = " + searcher.doc(doc.doc).get("id") + " (" + searcher.doc(doc.doc).get("triple_count") + ")" + doc.score + ", " + new LevensteinDistance().getDistance(name, searcher.doc(doc.doc).get("name")));
//            }
//
//            for (String suggest : spellChecker.suggestSimilar(name, 10, 0.5f)) {
//                System.out.println("---\n" + suggest);
//                docs = searcher.search(parser.parse(suggest), 10).scoreDocs;
//
//                for (ScoreDoc doc : docs) {
//                    if (doc.score / docs[0].score < 0.8) break;
//                    System.out.println(searcher.doc(doc.doc).get("name") + " = " + searcher.doc(doc.doc).get("id") + " (" + searcher.doc(doc.doc).get("triple_count") + ")" + doc.score + ", " + new LevensteinDistance().getDistance(name, searcher.doc(doc.doc).get("name")));
//                }
//            }
//
//
//
//    }
}