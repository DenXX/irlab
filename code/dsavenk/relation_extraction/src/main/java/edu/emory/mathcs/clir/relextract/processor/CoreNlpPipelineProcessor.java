package edu.emory.mathcs.clir.relextract.processor;

import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

/**
 * The processor runs standard Stanford CoreNLP pipeline to tag named entities
 * and resolves them to Freebase using the provided phrase-entity data.
 */
public abstract class CoreNlpPipelineProcessor extends Processor {

    private final StanfordCoreNLP nlpPipeline_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public CoreNlpPipelineProcessor(Properties properties) {
        super(properties);
        // Adds custom CoreNLP annotators.
        properties.setProperty("customAnnotatorClass.entityres",
                "edu.emory.mathcs.clir.relextract.annotators." +
                        "EntityResolutionAnnotator");
        properties.setProperty("customAnnotatorClass.span",
                "edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator");

        // Sets the NLP pipeline and some of the annotator properties.
        properties.put("annotators", getAnnotators());
        properties.setProperty("clean.allowflawedxml", "true");
        nlpPipeline_ = new StanfordCoreNLP(properties, false);
    }

    /**
     * Needs to be overriden to provide the list of Stanford CoreNLP annotators.
     *
     * @return a string containing the list of annotators for the NLP pipeline.
     */
    protected abstract String getAnnotators();

    @Override
    protected Annotation doProcess(Annotation document) {
        try {
            nlpPipeline_.annotate(document);
        } catch (IllegalArgumentException exc) {
            // cleanxml annotator throws exceptions if it doesn't like something
            // about xml inside the document text.
            return null;
        }
        return document;
    }
}
