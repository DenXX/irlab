package edu.emory.mathcs.clir.representations;

/**
 * Created by dsavenk on 4/23/15.
 */
public class WordVecTest {

    private String path_ = "/home/dsavenk/Projects/octiron/src/word2vec/GoogleNews-vectors-negative300.bin";

    @org.junit.Before
    public void setUp() throws Exception {}

    @org.junit.Test
    public void testFreebaseLinking() throws Exception {
        WordVec wordvec = new WordVec(path_);
    }

}
