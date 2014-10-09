package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.ling.CoreAnnotations;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.stanford.nlp.util.CoreMap;

import javax.xml.soap.Text;
import java.util.Properties;

/**
 * The processor runs standard Stanford CoreNLP pipeline to tag named entities
 * and resolves them to Freebase using the provided phrase-entity data.
 */
public class EntityAnnotationProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public EntityAnnotationProcessor(Properties properties) {
        super(properties);
        // Adds custom CoreNLP annotators.
        properties.setProperty("customAnnotatorClass.span",
                "edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator");


        // Sets the NLP pipeline and some of the annotator properties.
        properties.put("annotators", getAnnotators());
        properties.setProperty("clean.allowflawedxml", "true");
        nlpPipeline_ = new StanfordCoreNLP(properties, true);
    }

    protected String getAnnotators() {
        return "tokenize, cleanxml, ssplit, pos, lemma, ner, span";
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
            Document.Sentence.Builder sentBuilder =
                    Document.Sentence.newBuilder();
            sentBuilder.setFirstToken(sentence.get(
                    CoreAnnotations.TokenBeginAnnotation.class));
            sentBuilder.setLastToken(sentence.get(
                    CoreAnnotations.TokenEndAnnotation.class));
            sentBuilder.setText(sentence.get(
                    CoreAnnotations.TextAnnotation.class));
            docBuilder.addSentence(sentBuilder);
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

        return docBuilder.build();
    }

    private final StanfordCoreNLP nlpPipeline_;
}
