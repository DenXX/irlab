package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.DependencyTreeUtils;
import edu.emory.mathcs.clir.relextract.utils.PorterStemmer;
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
import edu.stanford.nlp.sentiment.SentimentCoreAnnotations;
import edu.stanford.nlp.trees.TreeCoreAnnotations;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.*;

import java.util.*;
import java.util.PriorityQueue;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 3/16/15.
 */
public class DocumentWrapper {

    private final Document.NlpDocument document_;
    private int questionSentsCount_ = -1;
    private Set<Integer> qWords_ = null;
    private Set<Integer> qVerbs_ = null;
    private Set<Integer> qFocus_ = null;
    private Set<String> qDepPaths_ = null;

    private static final String MEASURE_REGEX = "QUANTITY|CARDINAL|PERCENT|DATE|DURATION|TIME|SET|NUMBER|ORDINAL";

    private int[] mentionHead = null;
    IntervalTree<Integer, Interval<Integer>> mentionIntervals_ =
            new IntervalTree<>();
    Map<Interval, Pair<Document.Span, Integer>> intervalToMention_ = new HashMap<>();

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

    public int getTokenMentionHead(int token) {
        if (mentionHead == null) {
            saveTokenSpanMentions();
        }
        return mentionHead[token];
    }

    private void saveTokenSpanMentions() {
        mentionHead = new int[document().getTokenCount()];
        Arrays.fill(mentionHead, -1);

        for (Document.Span span : document().getSpanList()) {
            if ("MEASURE".equals(span.getType())
                    || (span.hasEntityId()
                        && span.getCandidateEntityScore(0) > Parameters.MIN_ENTITYID_SCORE)) {
                int mentionIndex = 0;
                for (Document.Mention mention : span.getMentionList()) {
                    int head = DependencyTreeUtils.getMentionHeadToken(document(), mention);
                    for (int j = mention.getTokenBeginOffset(); j < mention.getTokenEndOffset(); ++j) {
                        mentionHead[j] = head;
                    }
                    Interval<Integer> interval = Interval.toInterval(mention.getTokenBeginOffset(), mention.getTokenEndOffset() - 1);
                    mentionIntervals_.add(interval);
                    intervalToMention_.put(interval, new Pair<>(span, mentionIndex));
                    ++mentionIndex;
                }
            }
        }
    }

    public int getQuestionSentenceCount() {
        if (questionSentsCount_ != -1) return questionSentsCount_;

        for (int i = 0; i < document_.getSentenceCount(); ++i) {
            if (document_.getToken(document_.getSentence(i).getFirstToken()).getBeginCharOffset() >= document_.getQuestionLength())
                return i;
        }
        return questionSentsCount_ = document_.getSentenceCount();
    }


    public Set<String> getQuestionWords() {
        if (qWords_ == null) {
            findQWords();
        }
        return qWords_.stream().map(i -> document().getToken(i).getLemma().toLowerCase()).collect(Collectors.toSet());
    }

    public Set<String> getQuestionVerbs() {
        if (qVerbs_ == null) {
            findQVerbs();
        }
        return qVerbs_.stream().map(i -> document().getToken(i).getLemma().toLowerCase()).collect(Collectors.toSet());
    }

    public List<String> getQuestionLemmas(boolean replaceEntities) {
        PorterStemmer stemmer = new PorterStemmer();
        List<String> questionLemmas = new ArrayList<>();
        Document.Span lastTokenSpan = null;
        for (int token = 0; token < document_.getTokenCount()
                && document_.getToken(token).getBeginCharOffset() < document_.getQuestionLength(); ++token) {
            if (Character.isAlphabetic(document_.getToken(token).getPos().charAt(0)) && !document_.getToken(token).getPos().equals("DT")) {
                String term = document_.getToken(token).getText().toLowerCase();
                if (replaceEntities) {
                    List<Document.Span> tokenSpans = getTokenSpan(token);
                    if (!tokenSpans.isEmpty()) {
                        if (tokenSpans.get(0) != lastTokenSpan) {
                            term = tokenSpans.get(0).getNerType();
                        } else {
                            term = null;
                        }
                        lastTokenSpan = tokenSpans.get(0);
                    } else {
                        lastTokenSpan = null;
                    }
                }

                if (term != null && !term.isEmpty()) {
                    questionLemmas.add(stemmer.stem(term));
                }
            }
        }
        return questionLemmas;
    }

    public Set<String> getQuestionFocus() {
        if (qFocus_ == null) {
            qFocus_ = new HashSet<>();
            findQWords();

            qFocus_.addAll(qWords_.stream().filter(qWord -> document_.getToken(qWord).getDependencyGovernor() > 0).map(qWord -> document_.getSentence(document_.getToken(qWord).getSentenceIndex()).getFirstToken() + document_.getToken(qWord).getDependencyGovernor() - 1).collect(Collectors.toList()));

            for (int sentence = 0; sentence < getQuestionSentenceCount(); ++sentence) {
                for (int token = document_.getSentence(sentence).getFirstToken();
                     token < document_.getSentence(sentence).getLastToken(); ++token) {
                    int gov = document_.getSentence(sentence).getFirstToken() + document_.getToken(token).getDependencyGovernor() - 1;
                    if (document_.getToken(token).getPos().startsWith("N") && qWords_.contains(gov)) {
                        qFocus_.add(token);
                    }
                }
            }
        }
        return qFocus_.stream().map(i -> document().getToken(i).getLemma().toLowerCase()).collect(Collectors.toSet());
    }

    public Set<String> getQuestionDependencyPath(int targetToken) {
        if (qDepPaths_.isEmpty()) {
            findQWords();
            findQVerbs();
            Set<Integer> roots = qWords_.isEmpty() ? qVerbs_ : qWords_;
            for (int token : roots) {
                String path = DependencyTreeUtils.getDependencyPath(document_, token, targetToken, true, true, false);
                if (path != null) {
                    qDepPaths_.add(path);
                }
            }
        }
        return qDepPaths_;
    }

    @Override
    public String toString() {
        StringBuilder res = new StringBuilder();
        Map<Integer, List<Pair<Integer, Integer>>> beginToken2Mention = new HashMap<>();
        for (int spanIndex = 0; spanIndex < document_.getSpanCount(); ++spanIndex) {
            Document.Span span = document_.getSpan(spanIndex);
            for (int mentionIndex = 0; mentionIndex < span.getMentionCount(); ++mentionIndex) {
                Document.Mention mention = span.getMention(mentionIndex);
                int firstToken = mention.getTokenBeginOffset();
                if (!beginToken2Mention.containsKey(firstToken)) {
                    beginToken2Mention.put(firstToken, new ArrayList<>());
                }
                beginToken2Mention.get(firstToken).add(new Pair<>(spanIndex, mentionIndex));
            }
        }

        PriorityQueue<Triple<Integer, Integer, Integer>> currentMentions = new PriorityQueue<>();
        int prevSentenceIndex = 0;
        for (int tokenIndex = 0;
             tokenIndex < document_.getTokenCount(); ++tokenIndex) {
            if (document_.getToken(tokenIndex).getSentenceIndex() != prevSentenceIndex) {
                res.append("\n");
                prevSentenceIndex = document_.getToken(tokenIndex).getSentenceIndex();
            }
            if (beginToken2Mention.containsKey(tokenIndex)) {
                for (Pair<Integer, Integer> mention : beginToken2Mention.get(tokenIndex)) {
                    Document.Span span = document_.getSpan(mention.first);
                    Document.Mention spanMention = span.getMention(mention.second);
                    currentMentions.add(new Triple<>(spanMention.getTokenEndOffset(), mention.first, mention.second));
                    String spanTypeStr = (span.hasEntityId()
                            ? ":" + span.getEntityId()
                            : (span.getType().equals("MEASURE") ? ":" + span.getValue() : ""));
                    String mentionTypeStr = (spanMention.hasEntityId()
                            ? ":" + spanMention.getEntityId()
                            : (spanMention.getType().equals("MEASURE") ? ":" + spanMention.getValue() : ""));
                    String showReprMention = span.getRepresentativeMention() == mention.second
                            ? "!"
                            : "";
                    res.append("<" + showReprMention + mention.first + span.getType().charAt(0) + spanTypeStr + "|" +
                            spanMention.getType().charAt(0) + mentionTypeStr + " - ");
                }
            }
            res.append(document_.getToken(tokenIndex).getText() + " ");
            printMentionEnds(document_, currentMentions, tokenIndex, res);
        }
        printMentionEnds(document_, currentMentions, document_.getTokenCount(), res);
        return res.toString();
    }

    public boolean isTokenMeasure(int token) {
        return document().getToken(token).getNer().matches(MEASURE_REGEX);
    }

    private void printMentionEnds(Document.NlpDocument document, PriorityQueue<Triple<Integer, Integer, Integer>> currentMentions, int tokenIndex, StringBuilder res) {
        while (currentMentions.size() != 0) {
            Triple<Integer, Integer, Integer> nextMention = currentMentions.peek();
            if (nextMention.first - 1 <= tokenIndex) {
                res.append(nextMention.second + "> ");
                currentMentions.poll();
            } else {
                break;
            }
        }
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
                    .setText(sentence.get(CoreAnnotations.TextAnnotation.class));
            sentBuilder.setSentiment(sentence.get(SentimentCoreAnnotations.SentimentClass.class));
            if (sentence.has(TreeCoreAnnotations.TreeAnnotation.class)) {
                sentBuilder.setParseTree(sentence.get(TreeCoreAnnotations.TreeAnnotation.class).toString());
            }
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
                if (ner.matches(MEASURE_REGEX)) {
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

    private void findQWords() {
        qWords_ = new HashSet<>();
        for (int sentence = 0; sentence < getQuestionSentenceCount(); ++sentence) {
            for (int i = document_.getSentence(sentence).getFirstToken();
                 i < document_.getSentence(sentence).getLastToken(); ++i) {
                if (document_.getToken(i).getPos().startsWith("W")) {
                    qWords_.add(i);
                }
            }
            // We really want to include this early. Later sentences are unlikely to contain question words.
            if (qWords_.isEmpty()) {
                qWords_.add(document_.getSentence(sentence).getFirstToken());
            }
        }
    }

    private void findQVerbs() {
        qVerbs_ = new HashSet<>();
        for (int sentence = 0; sentence < getQuestionSentenceCount(); ++sentence) {
            for (int i = document_.getSentence(sentence).getFirstToken();
                 i < document_.getSentence(sentence).getLastToken(); ++i) {
                if (document_.getToken(i).getPos().startsWith("V") ||
                        document_.getToken(i).getPos().startsWith("MD")) {
                    qVerbs_.add(i);
                }
            }
        }
    }

    public List<Document.Span> getTokenSpan(int token) {
        if (mentionHead == null)
            saveTokenSpanMentions();
        return mentionIntervals_.getOverlapping(Interval.toInterval(token, token)).stream().map(x -> intervalToMention_.get(x).first).collect(Collectors.toList());
    }
}
