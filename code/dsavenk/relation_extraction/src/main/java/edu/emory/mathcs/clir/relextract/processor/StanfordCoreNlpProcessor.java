package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.dcoref.CoNLL2011DocumentReader;
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
import edu.stanford.nlp.util.Pair;

import java.util.*;

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

        Set<Integer> processedSpans = new HashSet<>();
        // Now let's process all coreference clusters.
        if (annotation.has(CorefCoreAnnotations.CorefChainAnnotation.class)) {
            for (CorefChain corefChain : annotation.get(
                    CorefCoreAnnotations.CorefChainAnnotation.class)
                    .values()) {

                // First I need to find a set of entities/measures that belong to this cluster.
                List<CoreMap> spans = new LinkedList<>();
                for (CorefChain.CorefMention mention :
                        corefChain.getMentionsInTextualOrder()) {
                    int sentenceFirstToken =
                            docBuilder.getSentence(mention.sentNum - 1)
                                    .getFirstToken();
                    int firstToken = sentenceFirstToken +
                            mention.startIndex - 1;
                    int endToken = sentenceFirstToken + mention.endIndex - 1;

                    int spanIndex = 0;
                    for (CoreMap span : annotation.get(
                            SpanAnnotator.SpanAnnotation.class)) {
                        int spanFirstToken = span.get(
                                CoreAnnotations.TokenBeginAnnotation.class);
                        int spanEndToken = span.get(
                                CoreAnnotations.TokenEndAnnotation.class);
                        if (spanFirstToken < endToken &&
                                firstToken < spanEndToken &&
                                !processedSpans.contains(spanIndex)) {
                            spans.add(span);
                            processedSpans.add(spanIndex);
                        }
                        ++spanIndex;
                    }
                }
                Document.Span.Builder spanBuilder = docBuilder.addSpanBuilder();
                String type;
                String nerType = "NONE";
                String value = "";
                int cnt = 1;
                for (CoreMap span : spans) {
                    if (span.has(CoreAnnotations.ValueAnnotation.class)) {
                        String newVal = span.get(CoreAnnotations.ValueAnnotation.class);
                        if (newVal.length() > value.length())
                            value = newVal;
                    }
                    if (nerType.equals(span.get(
                            CoreAnnotations.NamedEntityTagAnnotation.class))) {
                        ++cnt;
                    } else {
                        --cnt;
                        if (cnt == 0) {
                            nerType = span.get(
                                    CoreAnnotations.NamedEntityTagAnnotation.class);
                        }
                    }
                }
                switch (nerType) {
                    case "PERSON":
                    case "LOCATION":
                    case "ORGANIZATION":
                    case "MISC":
                        type = "ENTITY";
                        break;
                    case "OTHER":
                        type = "NONE";
                        break;
                    default:
                        type = "MEASURE";
                }
                spanBuilder.setType(type).setNerType(nerType).setValue(value);
                int mentionIndex = 0;
                for (CorefChain.CorefMention mention :
                        corefChain.getMentionsInTextualOrder()) {
                    int sentenceFirstToken =
                            docBuilder.getSentence(mention.sentNum - 1)
                                    .getFirstToken();
                    int firstToken = sentenceFirstToken +
                            mention.startIndex - 1;
                    int endToken = sentenceFirstToken + mention.endIndex - 1;
                    spanBuilder.addMentionBuilder()
                            .setTokenBeginOffset(firstToken)
                            .setTokenEndOffset(endToken)
                            .setSentenceIndex(mention.sentNum - 1)
                            .setText(mention.mentionSpan)
                            .setAnimacy(mention.animacy.name())
                            .setMentionType(mention.mentionType.name())
                            .setGender(mention.gender.name());
                    if (mention.equals(corefChain.getRepresentativeMention())) {
                        spanBuilder.setRepresentativeMention(mentionIndex);
                        // Set value to the value of the representative mention.
                        spanBuilder.setText(mention.mentionSpan);
                    }
                    ++mentionIndex;
                }
            }
        }

        // Process all Spans, that were not found in coreference clusters, if any.
        int spanIndex = 0;
        for (CoreMap span : annotation.get(SpanAnnotator.SpanAnnotation.class)) {
            if (!processedSpans.contains(spanIndex)) {
                Document.Span.Builder spanBuilder = docBuilder.addSpanBuilder();
                spanBuilder.setText(span.get(
                        CoreAnnotations.TextAnnotation.class));
                if (span.has(CoreAnnotations.ValueAnnotation.class)) {
                    spanBuilder.setValue(span.get(
                            CoreAnnotations.ValueAnnotation.class));
                } else {
                    spanBuilder.setValue(span.get(
                            CoreAnnotations.TextAnnotation.class));
                }
                spanBuilder.setNerType(
                        span.get(CoreAnnotations.NamedEntityTagAnnotation.class));
                switch (spanBuilder.getNerType()) {
                    case "PERSON":
                    case "LOCATION":
                    case "ORGANIZATION":
                    case "MISC":
                        spanBuilder.setType("ENTITY");
                        break;
                    default:
                        spanBuilder.setType("MEASURE");
                }
                int firstTokenIndex = span.get(
                        CoreAnnotations.TokenBeginAnnotation.class);
                int endTokenIndex = span.get(
                        CoreAnnotations.TokenEndAnnotation.class);

                Document.Mention.Builder mentionBuilder =
                        spanBuilder.addMentionBuilder()
                        .setTokenBeginOffset(firstTokenIndex)
                        .setTokenEndOffset(endTokenIndex)
                        .setSentenceIndex(docBuilder.getToken(span.get(
                                CoreAnnotations.TokenBeginAnnotation.class))
                                .getSentenceIndex())
                        .setText(span.get(CoreAnnotations.TextAnnotation.class));
                if (span.has(CoreAnnotations.ValueAnnotation.class)) {
                    mentionBuilder.setValue(span.get(CoreAnnotations.ValueAnnotation.class));
                } else {
                    mentionBuilder.setValue(span.get(CoreAnnotations.TextAnnotation.class));
                }
                mentionBuilder.setType(span.get(
                        CoreAnnotations.NamedEntityTagAnnotation.class));
            }
        }

        return docBuilder.build();
    }
}
