package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

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
        properties.setProperty("customAnnotatorClass.entityres",
                "edu.emory.mathcs.clir.relextract.annotators." +
                        "EntityResolutionAnnotator");
        properties.setProperty("customAnnotatorClass.span",
                "edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator");
        properties.setProperty("customAnnotatorClass.entityrel",
                "edu.emory.mathcs.clir.relextract.annotators." +
                        "EntityRelationsAnnotator");


        // Sets the NLP pipeline and some of the annotator properties.
        properties.put("annotators", getAnnotators());
        properties.setProperty("clean.allowflawedxml", "true");
        nlpPipeline_ = new StanfordCoreNLP(properties, false);
    }

    protected String getAnnotators() {
        return "tokenize, cleanxml, ssplit, pos, lemma, ner, span, entityres";
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
        return document;
    }

    private final StanfordCoreNLP nlpPipeline_;
}
