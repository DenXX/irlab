package edu.emory.mathcs.clir.entity;

import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.util.Map;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 4/23/15.
 */
public class FreebaseDictEntityLinking implements EntityLinking {

    private final KnowledgeBase kb_;

    public FreebaseDictEntityLinking(KnowledgeBase kb) {
        kb_ = kb;
    }

    @Override
    public Map<String, Float> resolveEntity(String name) {
        return kb_.lookupEntitiesByName(name)
                .stream()
                .collect(Collectors.toMap(mid -> mid, mid -> (float) kb_.getTripleCount(mid)));
    }
}
