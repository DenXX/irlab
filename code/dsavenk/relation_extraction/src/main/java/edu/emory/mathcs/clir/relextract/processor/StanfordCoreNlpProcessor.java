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
        properties.setProperty("parse.maxlen", "50");
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
                            dep.reln().getLongName());
                    if (dep.gov().index() == 0) {
                        sentBuilder.setDependencyRootToken(dep.dep().index());
                    }
                }
            }
        }

        for (CoreMap span : annotation.get(
                SpanAnnotator.SpanAnnotation.class)) {
            Document.Span.Builder spanBuilder = Document.Span.newBuilder();
            spanBuilder.setTokenBeginOffset(span.get(
                    CoreAnnotations.TokenBeginAnnotation.class));
            spanBuilder.setTokenEndOffset(span.get(
                    CoreAnnotations.TokenEndAnnotation.class));
            spanBuilder.setSentenceIndex(docBuilder.getToken(span.get(
                    CoreAnnotations.TokenBeginAnnotation.class))
                    .getSentenceIndex());
            spanBuilder.setText(span.get(CoreAnnotations.TextAnnotation.class));
            if (span.has(CoreAnnotations.ValueAnnotation.class)) {
                spanBuilder.setValue(span.get(CoreAnnotations.ValueAnnotation.class));
            } else {
                spanBuilder.setValue(span.get(CoreAnnotations.TextAnnotation.class));
            }
            spanBuilder.setType(span.get(
                    CoreAnnotations.NamedEntityTagAnnotation.class));
            // The following fields are supposed to be filled by coreference
            // resolver: mentionType, mentionNumber, gender, animacy

            docBuilder.addSpan(spanBuilder);
        }

        // Now let's process all coreference clusters.
        if (annotation.has(CorefCoreAnnotations.CorefChainAnnotation.class)) {
            int index = 0;
            for (CorefChain corefChain : annotation.get(
                    CorefCoreAnnotations.CorefChainAnnotation.class)
                    .values()) {
                Document.CorefCluster.Builder corefBuilder =
                        docBuilder.addCorefClusterBuilder();
                int mentionIndex = 0;
                for (CorefChain.CorefMention mention :
                        corefChain.getMentionsInTextualOrder()) {
                    corefBuilder.addMentionBuilder()
                            .setTokenBeginOffset(mention.startIndex)
                            .setTokenEndOffset(mention.endIndex)
                            .setCorefClusterIndex(index)
                            .setSentenceIndex(mention.sentNum)
                            .setText(mention.mentionSpan)
                            .setAnimacy(mention.animacy.name())
                            .setMentionType(mention.mentionType.name())
                            .setGender(mention.gender.name());
                    if (mention.equals(corefChain.getRepresentativeMention())) {
                        corefBuilder.setRepresentativeMention(mentionIndex);
                    }
                    ++mentionIndex;
                }
                ++index;
            }
        }

        return docBuilder.build();
    }
}
