package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.stanford.nlp.pipeline.Annotation;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

import java.util.Properties;

/**
 * The processor runs standard Stanford CoreNLP pipeline.
 */
public class StanfordCoreNlpProcessor extends Processor {
    private final StanfordCoreNLP nlpPipeline_;

    public static final String CASELESS_MODELS_PARAMETER = "caselessnlp";

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
        properties.setProperty("customAnnotatorClass.answer_mentions",
                "edu.emory.mathcs.clir.relextract.annotators.AddAnswerMentionsAnnotator");
        properties.setProperty("customAnnotatorClass.moddcoref",
                "edu.emory.mathcs.clir.relextract.annotators.ModifiedDeterministicCorefAnnotator");


        // Sets the NLP pipeline and some of the annotator properties.
        properties.put("annotators", getAnnotators());
        // This seems like the right thing to do, but there is a problem (null
        // pointer exception, because newline chars are not removed then. Can
        // fix that, but from the other side, maybe I shouldn't be doing this.
        //properties.setProperty("ssplit.newlineIsSentenceBreak", "always");
        properties.setProperty("clean.allowflawedxml", "true");
        // Use much faster shift-reduce parser.
        properties.setProperty("parse.model",
                "edu/stanford/nlp/models/srparser/englishSR.ser.gz");
        properties.setProperty("parse.buildgraphs", "false");
        // Do not allow reparsing as it wasn't supported with shift-reduce
        // parser.
        properties.setProperty("dcoref.allowReparsing", "false");
        // if set, the annotator parses only sentences shorter (in terms of
        // number of tokens) than this number. For longer sentences, the parser
        // creates a flat structure, where every token is assigned to
        // the non-terminal X. This is useful when parsing noisy web text,
        // which may generate arbitrarily long sentences.
        properties.setProperty("parse.maxlen", "50");

        // Let CoreNLP use single thread inside, we have parallelism on top.
        properties.setProperty("nthreads", "1");
        // Default value is 0 if nthreads is not 1, thus no predictions made.
        properties.setProperty("ner.maxtime", "-1");
        // Load big file with gender and number information.
        properties.setProperty("dcoref.use.big.gender.number", "true");

        // With caseless model we have fewer named entities, so staying with the previous one.
        if (properties.containsKey(CASELESS_MODELS_PARAMETER)) {
            properties.setProperty("ner.model",
                    "edu/stanford/nlp/models/ner/english.muc.7class.caseless.distsim.crf.ser.gz");
        }

        // Post-processing removes singletons, let's keep them in order to get
        // more mentions and hopefully more relations.
        //properties.setProperty("dcoref.postprocessing", "true");
        nlpPipeline_ = new StanfordCoreNLP(properties, true);
    }

    protected String getAnnotators() {
        return "tokenize, ssplit, pos, lemma, ner, entitymentions, parse, depparse, sentiment";
        //return "tokenize, ssplit, pos, lemma, ner, entitymentions, parse, depparse, moddcoref";
        //return "tokenize, ssplit, pos, lemma, ner, regexner, entitymentions, parse, depparse, moddcoref";
        //return "tokenize, ssplit, pos, lemma, ner, regexner, entitymentions, parse, depparse, answer_mentions, moddcoref";
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

        return new DocumentWrapper(document, annotation).document();
    }
}
