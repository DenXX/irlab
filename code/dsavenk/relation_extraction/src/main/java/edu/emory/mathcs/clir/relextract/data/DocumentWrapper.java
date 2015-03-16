package edu.emory.mathcs.clir.relextract.data;

import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.dcoref.Dictionaries;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.IndexedWord;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.semgraph.SemanticGraphEdge;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Interval;
import edu.stanford.nlp.util.IntervalTree;
import edu.stanford.nlp.util.Pair;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

/**
 * Created by dsavenk on 3/16/15.
 */
public class DocumentWrapper {

    private final Document.NlpDocument document_;
    private int questionSentsCount_ = -1;

    /**
     * Create document wrapper for the given document.
     * @param document NlpDocument proto containing the document.
     */
    public DocumentWrapper(Document.NlpDocument document) {
        document_ = document;
    }

    /**
     * Convert the given CoreNlp annotation to NlpDocument proto.
     * @param document Stanford CoreNlp document annotation.
     */
    public DocumentWrapper(Document.NlpDocument document, Annotation annotation) {
        document_ = convertCoreNlpAnnotationToDocument(document, annotation);
    }


    /**
     * For now we will give access to the document itself. Later we should probably prohibit this.
     * @return
     */
    public Document.NlpDocument document() {
        return document_;
    }


    public int getQuestionSentenceCount() {
        if (questionSentsCount_ != -1) return questionSentsCount_;

        for (int i = 0; i < document_.getSentenceCount(); ++i) {
            if (document_.getToken(document_.getSentence(i).getFirstToken()).getBeginCharOffset() >= document_.getQuestionLength())
                return i;
        }
        return questionSentsCount_ = document_.getSentenceCount();
    }


    /**
     * Converts Stanford CoreNlp annotation object to the proto format and stores it in the wrapper.
     * @param annotation
     * @return
     */
    private Document.NlpDocument convertCoreNlpAnnotationToDocument(Document.NlpDocument document, Annotation annotation) {
        Document.NlpDocument.Builder docBuilder = document.toBuilder();
        docBuilder.clearSentence().clearSpan().clearToken();

        // Build tokens.
        for (CoreLabel token : annotation.get(
                CoreAnnotations.TokensAnnotation.class)) {
            Document.Token.Builder tokenBuilder = docBuilder.addTokenBuilder();
            tokenBuilder.setText(token.get(
                    CoreAnnotations.TextAnnotation.class));
            tokenBuilder.setBeginCharOffset(token.get(
                    CoreAnnotations.CharacterOffsetBeginAnnotation.class));
            tokenBuilder.setEndCharOffset(token.get(
                    CoreAnnotations.CharacterOffsetEndAnnotation.class));
            tokenBuilder.setOriginalText(token.get(
                    CoreAnnotations.OriginalTextAnnotation.class));
            tokenBuilder.setLemma(token.get(
                    CoreAnnotations.LemmaAnnotation.class));
            tokenBuilder.setSentenceIndex(token.get(
                    CoreAnnotations.SentenceIndexAnnotation.class));
            tokenBuilder.setPos(token.get(
                    CoreAnnotations.PartOfSpeechAnnotation.class));
            tokenBuilder.setNer(token.get(
                    CoreAnnotations.NamedEntityTagAnnotation.class));
            if (token.has(
                    CoreAnnotations.NormalizedNamedEntityTagAnnotation.class)) {
                tokenBuilder.setNormalizedNer(token.get(
                        CoreAnnotations.NormalizedNamedEntityTagAnnotation.class));
            } else {
                tokenBuilder.setNormalizedNer(token.get(
                        CoreAnnotations.NamedEntityTagAnnotation.class));
            }
            tokenBuilder.setWhitespaceBefore(token.get(
                    CoreAnnotations.BeforeAnnotation.class));
            tokenBuilder.setWhitespaceAfter(token.get(
                    CoreAnnotations.AfterAnnotation.class));
        }

        // Build sentences.
        for (CoreMap sentence : annotation.get(
                CoreAnnotations.SentencesAnnotation.class)) {

            int firstSentenceToken = sentence.get(
                    CoreAnnotations.TokenBeginAnnotation.class);
            int endSentenceToken = sentence.get(
                    CoreAnnotations.TokenEndAnnotation.class);
            Document.Sentence.Builder sentBuilder =
                    docBuilder.addSentenceBuilder();
            sentBuilder.setFirstToken(firstSentenceToken)
                    .setLastToken(endSentenceToken)
                    .setText(sentence.get(CoreAnnotations.TextAnnotation.class))
                    .setParseTree(sentence.get(TreeCoreAnnotations.TreeAnnotation.class).toString());
            // Process dependency tree.
            // TODO(denxx): I switched to the basic dependency tree, so that I
            // have no loops and everything is a tree.
            if (sentence.has(SemanticGraphCoreAnnotations
                    .BasicDependenciesAnnotation.class)) {
                SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations
                        .BasicDependenciesAnnotation.class);
                Queue<IndexedWord> q = new LinkedList<>(graph.getRoots());
                int[] depths = new int[endSentenceToken - firstSentenceToken];
                boolean[] visited = new boolean[
                        endSentenceToken - firstSentenceToken];

                for (TypedDependency dep : graph.typedDependencies()) {
                    sentBuilder.setDependencyTree(graph.toString());
                    docBuilder.getTokenBuilder(firstSentenceToken
                            + dep.dep().index() - 1).setDependencyGovernor(
                            dep.gov().index()).setDependencyType(
                            dep.reln().getShortName());
                    if (dep.gov().index() == 0) {
                        sentBuilder.setDependencyRootToken(dep.dep().index());
                        // Set depth of the root.
                        docBuilder.getTokenBuilder(firstSentenceToken
                                + dep.dep().index() - 1)
                                .setDependencyTreeNodeDepth(0);
                        visited[dep.dep().index() - 1] = true;
                    }
                }

                // Set depth of other nodes in the graph.
                while (!q.isEmpty()) {
                    IndexedWord w = q.poll();
                    for (IndexedWord child : graph.getChildren(w)) {
                        if (!visited[child.index() - 1]) {
                            visited[child.index() - 1] = true;
                            depths[child.index() - 1] = depths[w.index() - 1] + 1;
                            docBuilder.getTokenBuilder(firstSentenceToken
                                    + child.index() - 1).setDependencyTreeNodeDepth(
                                    depths[child.index() - 1]);
                            q.add(child);
                        }
                    }
                }
            }

            // Saving the full graph
            if (sentence.has(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class)) {
                SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class);
                for (SemanticGraphEdge edge : graph.edgeIterable()) {
                    sentBuilder.addDependencyEdgeBuilder().setSource(
                            edge.getSource().index()).setTarget(edge.getTarget().index()).setLabel(edge.getRelation().getShortName());
                }
            }
        }

        // Process spans and coreference clusters.

        // First we create a span and mentions for all coreference clusters.
        Map<Interval<Integer>, Pair<Integer, Integer>> intervalToMention =
                new HashMap<>();
        IntervalTree<Integer, Interval<Integer>> mentionIntervals =
                new IntervalTree<>();
        int corefIndex = 0;

        if (annotation.containsKey(CorefCoreAnnotations.CorefChainAnnotation.class)) {
            for (CorefChain corefCluster :
                    annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class)
                            .values()) {
                if (corefCluster.getRepresentativeMention() == null) continue;
                boolean keep = false;
                for (CorefChain.CorefMention mention :
                        corefCluster.getMentionsInTextualOrder()) {
                    // Don't want to keep clusters with only prononinal mentions
                    if (mention.mentionType != Dictionaries.MentionType.PRONOMINAL &&
                            !mention.mentionSpan.equals("this")) {
                        keep = true;
                    }
                }
                // If this cluster doesn't have any nominal mentions, remove it.
                if (!keep) continue;

                String spanName = PTBTokenizer.ptb2Text(
                        corefCluster.getRepresentativeMention().mentionSpan);

                Document.Span.Builder spanBuilder = docBuilder.addSpanBuilder();

                spanBuilder.setText(spanName)
                        .setValue(spanName)
                        .setType("OTHER")
                        .setNerType("NONE");

                // Add all mentions.
                int mentionIndex = 0;
                for (CorefChain.CorefMention mention :
                        corefCluster.getMentionsInTextualOrder()) {
                    String mentionText = PTBTokenizer.ptb2Text(mention.mentionSpan);

                    int sentenceFirstToken =
                            docBuilder.getSentence(mention.sentNum - 1)
                                    .getFirstToken();
                    int firstToken = sentenceFirstToken +
                            mention.startIndex - 1;
                    int endToken = sentenceFirstToken + mention.endIndex - 1;
                    spanBuilder.addMentionBuilder()
                            .setText(mentionText)
                            .setValue(mentionText)
                            .setType("OTHER")
                            .setSentenceIndex(mention.sentNum - 1)
                            .setTokenBeginOffset(firstToken)
                            .setTokenEndOffset(endToken)
                            .setGender(mention.gender.name())
                            .setAnimacy(mention.animacy.name())
                            .setNumber(mention.number.name())
                            .setMentionType(mention.mentionType.name());
                    Interval<Integer> interval = Interval.toInterval(firstToken,
                            endToken - 1);
                    intervalToMention.put(interval, new Pair<>(corefIndex,
                            mentionIndex));
                    mentionIntervals.add(interval);
                    if (mention.equals(corefCluster.getRepresentativeMention())) {
                        spanBuilder.setRepresentativeMention(mentionIndex);
                    }
                    ++mentionIndex;
                }
                ++corefIndex;
            }
        }

        // Now go over all spans and find the span, that works best for them.
        if (annotation.containsKey(CoreAnnotations.MentionsAnnotation.class)) {
            for (CoreMap span : annotation.get(
                    CoreAnnotations.MentionsAnnotation.class)) {

                final String ner = span.get(
                        CoreAnnotations.NamedEntityTagAnnotation.class);
                String type;
                if (ner.matches("QUANTITY|CARDINAL|PERCENT|DATE|DURATION|TIME|SET|NUMBER|ORDINAL")) {
                    type = "MEASURE";
                } else {
                    type = "ENTITY";
                }

                int firstToken = span.get(CoreAnnotations.TokenBeginAnnotation.class);
                int endToken = span.get(CoreAnnotations.TokenEndAnnotation.class);
                Interval<Integer> spanInterval =
                        Interval.toInterval(firstToken, endToken - 1);
                // Let's find the tightest mention interval, that cover the given
                // span.
                Interval<Integer> bestInterval = null;
                int bestScore = Integer.MAX_VALUE;
                for (Interval<Integer> mention :
                        mentionIntervals.getOverlapping(spanInterval)) {
                    int score = Math.abs(spanInterval.getBegin() - mention.getBegin()) +
                            Math.abs(spanInterval.getEnd() - mention.getEnd());
                    if (bestInterval == null || score < bestScore) {
                        bestInterval = mention;
                        bestScore = score;
                    }
                }

                Document.Span.Builder spanBuilder;
                Document.Mention.Builder mentionBuilder;
                // If we didn't find any interval, we will create a new span.
                if (bestInterval == null) {
                    spanBuilder = docBuilder.addSpanBuilder();
                    mentionBuilder = spanBuilder.addMentionBuilder();
                    spanBuilder.setRepresentativeMention(0);
                } else {
                    Pair<Integer, Integer> mention =
                            intervalToMention.get(bestInterval);
                    spanBuilder = docBuilder.getSpanBuilder(mention.first);

                    // If span interval exactly equals the given one, then we reuse,
                    // otherwise we create a new mention.
                    if (bestInterval.equals(spanInterval)) {
                        mentionBuilder = spanBuilder.getMentionBuilder(
                                mention.second);
                    } else {
                        mentionBuilder = spanBuilder.addMentionBuilder();
                    }
                }

                spanBuilder
                        .setText(span.get(CoreAnnotations.TextAnnotation.class))
                        .setValue(span.has(CoreAnnotations.ValueAnnotation.class)
                                ? span.get(CoreAnnotations.ValueAnnotation.class)
                                : span.get(CoreAnnotations.TextAnnotation.class))
                        .setType(type)
                        .setNerType(ner);
                // Overwrite everything for the mention.
                // TODO(denxx): Should I pick the best value is there are multiple?
                mentionBuilder
                        .setText(span.get(CoreAnnotations.TextAnnotation.class))
                        .setValue(span.has(CoreAnnotations.ValueAnnotation.class)
                                ? span.get(CoreAnnotations.ValueAnnotation.class)
                                : span.get(CoreAnnotations.TextAnnotation.class))
                        .setType(ner)
                        .setSentenceIndex(docBuilder.getToken(span.get(
                                CoreAnnotations.TokenBeginAnnotation.class))
                                .getSentenceIndex())
                        .setTokenBeginOffset(firstToken)
                        .setTokenEndOffset(endToken)
                        .setMentionType(type.equals("ENTITY") ? "NOMINAL" :
                                "VALUE");
            }
        }

        return docBuilder.build();
    }
}