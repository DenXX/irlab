package edu.emory.mathcs.clir.entity;

import java.util.Map;

/**
 * Created by dsavenk on 4/23/15.
 */
public interface EntityLinking {

    Map<String, Float> resolveEntity(String name);
}
