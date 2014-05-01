package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.QuestionAnswerPair;
import org.apache.commons.collections4.keyvalue.MultiKey;
import org.apache.commons.collections4.map.MultiKeyMap;

import java.util.*;

/**
 * Created by dsavenk on 4/30/14.
 */
public class EntityPairCollectorProcessor implements QuestionAnswerPairProcessor {
    MultiKeyMap<String, List<QuestionAnswerPair>> entityPairs = new MultiKeyMap<String, List<QuestionAnswerPair>>();
    int counter = 0;

    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair qa) {
        String questionEntity = qa.getAttribute("question_entity");
        String answerEntity = qa.getAttribute("answer_entity");
        if (!entityPairs.containsKey(questionEntity, answerEntity)) {
            entityPairs.put(questionEntity, answerEntity, new ArrayList<QuestionAnswerPair>());
        }
        entityPairs.get(questionEntity, answerEntity).add(qa);
        if ((++counter % 100000) == 0) {
            System.out.println("-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=-=");
            printPairs();
        }
        return qa;
    }

    public void printPairs() {
        List<Map.Entry<MultiKey<? extends String>, List<QuestionAnswerPair>>> entries =
                new ArrayList<Map.Entry<MultiKey<? extends String>, List<QuestionAnswerPair>>>(entityPairs.entrySet());
        Collections.sort(entries, new Comparator<Map.Entry<MultiKey<? extends String>, List<QuestionAnswerPair>>>() {
            @Override
            public int compare(Map.Entry<MultiKey<? extends String>, List<QuestionAnswerPair>> a, Map.Entry<MultiKey<? extends String>, List<QuestionAnswerPair>> b) {
                if (a.getValue().size() < b.getValue().size()) {
                    return 1;
                } else if (a.getValue().size() > b.getValue().size()) {
                    return -1;
                } else {
                    return 0;
                }
            }
        });
        for (Map.Entry<MultiKey<? extends String>, List<QuestionAnswerPair>> entry : entries) {
            System.out.print("\n======================================================================================\n");
            System.out.print(entry.getKey().getKey(0) + " <=> " + entry.getKey().getKey(1));
            if (entry.getValue().get(0).hasAttribute("predicate")) {
                System.out.print(" [" + entry.getValue().get(0).getAttribute("predicate") + " ]");
            }
            System.out.print("\n\n--------- QUESTIONS (" + entry.getValue().size() + ")\n\n");
            for (QuestionAnswerPair qa : entry.getValue()) {
                System.out.println(qa);
                System.out.print("\n---------------------------\n\n");
            }
        }
    }
}
