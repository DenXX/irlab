package edu.emory.mathcs.clir.relextract.edu.emory.mathcs.clir.entity;

import edu.stanford.nlp.util.Pair;

import java.util.List;

/**
 * Created by dsavenk on 4/23/15.
 */
public interface EntityLinking {

    List<Pair<String, Float>> resolveEntity(String name);
}
