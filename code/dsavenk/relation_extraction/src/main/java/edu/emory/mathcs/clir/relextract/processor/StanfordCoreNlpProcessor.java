package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.dcoref.CorefChain;
import edu.stanford.nlp.dcoref.CorefCoreAnnotations;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.semgraph.SemanticGraph;
import edu.stanford.nlp.semgraph.SemanticGraphCoreAnnotations;
import edu.stanford.nlp.trees.TypedDependency;
import edu.stanford.nlp.util.CoreMap;
import edu.stanford.nlp.util.Interval;
import edu.stanford.nlp.util.IntervalTree;
import edu.stanford.nlp.util.Pair;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * The processor runs standard Stanford CoreNLP pipeline to tag named entities
 * and resolves them to Freebase using the provided phrase-entity data.
 */
public class StanfordCoreNlpProcessor extends Processor {
    private final StanfordCoreNLP nlpPipeline_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public StanfordCoreNlpProcessor(Properties properties) {
        super(properties);
        // Adds custom CoreNLP annotators.
        properties.setProperty("customAnnotatorClass.span",
                "edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator");


        // Sets the NLP pipeline and some of the annotator properties.
        properties.put("annotators", getAnnotators());
        properties.setProperty("clean.allowflawedxml", "true");
        // Use much faster shift-reduce parser.
        properties.setProperty("parse.model",
                "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        // Do not allow reparsing as it wasn't supported with shift-reduce
        // parser.
        properties.setProperty("dcoref.allowReparsing", "false");
        // if set, the annotator parses only sentences shorter (in terms of
        // number of tokens) than this number. For longer sentences, the parser
        // creates a flat structure, where every token is assigned to
        // the non-terminal X. This is useful when parsing noisy web text,
        // which may generate arbitrarily long sentences.
        //properties.setProperty("parse.maxlen", "50");
        properties.setProperty("dcoref.postprocessing", "true");
        nlpPipeline_ = new StanfordCoreNLP(properties, true);
    }

    protected String getAnnotators() {
        return "tokenize, cleanxml, ssplit, pos, lemma, ner, span, parse, dcoref";
    }

    @Override
    public Document.NlpDocument doProcess(Document.NlpDocument document) {
        Annotation annotation = new Annotation(document.getText());
        try {
            nlpPipeline_.annotate(annotation);
        } catch (IllegalArgumentException exc) {
            // cleanxml annotator throws exceptions if it doesn't like something
            // about xml inside the document text.
            System.err.println(exc);
            return null;
        }
        Document.NlpDocument.Builder docBuilder = document.toBuilder();

        // Build tokens.
        for (CoreLabel token : annotation.get(
                CoreAnnotations.TokensAnnotation.class)) {
            Document.Token.Builder tokenBuilder = Document.Token.newBuilder();
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
            docBuilder.addToken(tokenBuilder);
        }

        // Build sentences.
        for (CoreMap sentence : annotation.get(
                CoreAnnotations.SentencesAnnotation.class)) {
            int firstSentenceToken = sentence.get(
                    CoreAnnotations.TokenBeginAnnotation.class);
            Document.Sentence.Builder sentBuilder =
                    Document.Sentence.newBuilder();
            sentBuilder.setFirstToken(firstSentenceToken);
            sentBuilder.setLastToken(sentence.get(
                    CoreAnnotations.TokenEndAnnotation.class));
            sentBuilder.setText(sentence.get(
                    CoreAnnotations.TextAnnotation.class));
            docBuilder.addSentence(sentBuilder);
            // Process dependency tree.
            if (sentence.has(
                    SemanticGraphCoreAnnotations
                            .CollapsedCCProcessedDependenciesAnnotation
                            .class)) {
                SemanticGraph graph = sentence.get(SemanticGraphCoreAnnotations
                        .CollapsedCCProcessedDependenciesAnnotation
                        .class);
                for (TypedDependency dep : graph.typedDependencies()) {
                    sentBuilder.setDependencyTree(graph.toString());
                    docBuilder.getTokenBuilder(firstSentenceToken
                            + dep.dep().index() - 1).setDependencyGovernor(
                            dep.gov().index()).setDependencyType(
                            dep.reln().getShortName());
                    if (dep.gov().index() == 0) {
                        sentBuilder.setDependencyRootToken(dep.dep().index());
                    }
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
        for (CorefChain corefCluster :
                annotation.get(CorefCoreAnnotations.CorefChainAnnotation.class)
                        .values()) {
            if (corefCluster.getRepresentativeMention() == null) continue;

            Document.Span.Builder spanBuilder = docBuilder.addSpanBuilder();
            spanBuilder
                    .setText(corefCluster.getRepresentativeMention()
                            .mentionSpan)
                    .setValue(corefCluster.getRepresentativeMention()
                            .mentionSpan)
                    .setType("OTHER")
                    .setNerType("NONE");

            // Add all mentions.
            int mentionIndex = 0;
            for (CorefChain.CorefMention mention :
                    corefCluster.getMentionsInTextualOrder()) {
                int sentenceFirstToken =
                        docBuilder.getSentence(mention.sentNum - 1)
                                .getFirstToken();
                int firstToken = sentenceFirstToken +
                        mention.startIndex - 1;
                int endToken = sentenceFirstToken + mention.endIndex - 1;
                spanBuilder.addMentionBuilder()
                        .setText(mention.mentionSpan)
                        .setValue(mention.mentionSpan)
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

        // Now go over all spans and find the span, that works best for them.
        for (CoreMap span : annotation.get(
                SpanAnnotator.SpanAnnotation.class)) {

            final String ner = span.get(
                    CoreAnnotations.NamedEntityTagAnnotation.class);
            String type;
            if ("PERSON".equals(ner) || "ORGANIZATION".equals(ner) ||
                    "LOCATION".equals(ner) || "MISC".equals(ner)) {
                type = "ENTITY";
            } else {
                type = "MEASURE";
            }

            int firstToken = span.get(CoreAnnotations.TokenBeginAnnotation.class);
            int endToken = span.get(CoreAnnotations.TokenEndAnnotation.class);
            Interval<Integer> spanInterval =
                    Interval.toInterval(firstToken, endToken - 1);
            // Let's find the tightest mention interval, that cover the given
            // span.
            Interval<Integer> tightestInterval = null;
            for (Interval<Integer> mention :
                    mentionIntervals.getOverlapping(spanInterval)) {
                if (tightestInterval == null ||
                        (tightestInterval.getEnd() - tightestInterval.getBegin()
                                > mention.getEnd() - mention.getBegin())) {
                    tightestInterval = mention;
                }
            }

            Document.Span.Builder spanBuilder;
            Document.Mention.Builder mentionBuilder;
            // If we didn't find any interval, we will create a new span.
            if (tightestInterval == null) {
                spanBuilder = docBuilder.addSpanBuilder();
                mentionBuilder = spanBuilder.addMentionBuilder();
                spanBuilder.setRepresentativeMention(0);
            } else {
                Pair<Integer, Integer> mention =
                        intervalToMention.get(tightestInterval);
                spanBuilder = docBuilder.getSpanBuilder(mention.first);
                // If span interval exactly equals the given one, then we reuse,
                // otherwise we creare a new one.
                if (tightestInterval.equals(spanInterval)) {
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

        return docBuilder.build();
    }
}
