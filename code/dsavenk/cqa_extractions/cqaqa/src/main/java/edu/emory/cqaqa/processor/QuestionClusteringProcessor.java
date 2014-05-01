package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.CqaPost;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by dsavenk on 4/29/14.
 */
public class QuestionClusteringProcessor implements CqaPostProcessor {
    HashMap<CqaPost, List<CqaPost>> clusters = new HashMap<CqaPost, List<CqaPost>>();

    int counter = 0;

    @Override
    public CqaPost processPost(CqaPost post) {
        if (!clusters.containsKey(post)) {
            clusters.put(post, new ArrayList<CqaPost>());
        }
        clusters.get(post).add(post);
        if ((++counter) % 10000 == 0) {
            exploreClusters();
        }
        return post;
    }

    public void exploreClusters() {
        System.out.println("=========================================================================================");
        for (Map.Entry<CqaPost, List<CqaPost>> entry : clusters.entrySet()) {
            if (entry.getValue().size() > 5) {
                System.out.println(">>>>>>>>>>>>> CLUSTER size = " + entry.getValue().size());
                for (int i = 0; i < entry.getValue().size(); ++i) {
                    System.out.println(entry.getValue().get(i));
                }
            }
        }
    }
}

