package edu.emory.mathcs.clir.relextract.utils;

import com.hp.hpl.jena.rdf.model.StmtIterator;

import java.util.Properties;

import static org.junit.Assert.assertTrue;

public class KnowledgeBaseTest {

    private KnowledgeBase kb_;

    @org.junit.Before
    public void setUp() throws Exception {
        // TODO(denxx): Create small model and put it to the resources.
        kb_ = KnowledgeBase.getInstance(new Properties());
    }

    @org.junit.Test
    public void testGetSubjectTriples() throws Exception {
        StmtIterator iter = kb_.getSubjectTriples("/m/sddadf");
        assertTrue(iter.hasNext());
    }

    @org.junit.Test
    public void testGetSubjectObjectTriples() throws Exception {

    }
}