package edu.emory.mathcs.clir.relextract.annotators;

import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.MultiTokenTag;
import edu.stanford.nlp.ling.tokensregex.types.Tags;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.Annotator;
import edu.stanford.nlp.util.ArraySet;

import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Created by dsavenk on 4/1/15.
 */
public class AddAnswerMentionsAnnotator implements Annotator {

    public static final String ANNOTATOR_CLASS = "answer_mentions";
    public static final Requirement ANSWER_SPAN_REQUIREMENT =
            new Requirement(ANNOTATOR_CLASS);

    public AddAnswerMentionsAnnotator(String annotatorClass, Properties props) {
    }

    @Override
    public void annotate(Annotation annotation) {
        int tokenIndex = 0;
        int mentionStarted = -1;
        for (CoreLabel token : annotation.get(CoreAnnotations.TokensAnnotation.class)) {
            String text = token.get(CoreAnnotations.TextAnnotation.class);
            if (text.charAt(0) == '<' && text.charAt(text.length() - 1) == '>') {
                token.set(CoreAnnotations.TextAnnotation.class, text.substring(1, text.length() - 1));
                token.set(CoreAnnotations.MentionTokenAnnotation.class, new MultiTokenTag(new MultiTokenTag.Tag("answer", "ANS", text.length()), tokenIndex));
            }
//                mentionStarted = tokenIndex;
//            } else if (token.get(CoreAnnotations.TextAnnotation.class).equals(">")) {
//                w.get(CoreAnnotations.MentionTokenAnnotation.class
//            }

            ++tokenIndex;
        }

    }

    @Override
    public Set<Requirement> requirementsSatisfied() {
        return Collections.singleton(ANSWER_SPAN_REQUIREMENT);
    }

    @Override
    public Set<Requirement> requires() {
        return new ArraySet<>(NER_REQUIREMENT);
    }
}
