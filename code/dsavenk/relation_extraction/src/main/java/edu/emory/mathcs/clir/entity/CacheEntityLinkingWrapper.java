package edu.emory.mathcs.clir.entity;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dsavenk on 4/23/15.
 */
public class CacheEntityLinkingWrapper implements EntityLinking {
    private final Map<String, Map<String, Float>> cache_ = Collections.synchronizedMap(new HashMap<>());
    private final EntityLinking linker_;

    public CacheEntityLinkingWrapper(EntityLinking baseLinker) {
        linker_ = baseLinker;
    }

    @Override
    public Map<String, Float> resolveEntity(String name) {
        cache_.putIfAbsent(name, linker_.resolveEntity(name));
        return cache_.get(name);
    }
}
