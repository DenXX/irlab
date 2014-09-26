package edu.emory.mathcs.clir.relextract.processor;

import edu.stanford.nlp.pipeline.Annotation;

import java.util.Properties;

/**
 * Abstract class that represents a document processor. This is very similar to
 * CoreNLP annotator, but allows document filtering, etc. We will encapsulate
 * NLP processing and some other manipulations as processors.
 */
public abstract class Processor {

    /**
     * A state of the current processor. Each processor should be frozen before
     * any calls to the process method. The process method can be called from
     * different threads and needs to be thread-safe.
     */
    private boolean frozen_ = false;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public Processor(Properties properties) {
    }

    /**
     * The actual method that does document processing. Each subclass will need
     * to override this method. The method is called from the public process
     * method that does some additional checks to make sure the processor is
     * thread-safe.
     *
     * @param document A document to process.
     * @return Processed document or null if the document is filtered.
     */
    protected abstract Annotation doProcess(Annotation document)
            throws Exception;

    /**
     * Freezes the processor, which means no further changes in state are
     * allowed and object is thread-safe.
     *
     * @throws IllegalStateException if processor is already frozen.
     */
    public void freeze() throws IllegalStateException {
        if (frozen_) {
            throw new IllegalStateException("The processor is already frozen");
        }
        frozen_ = true;
    }

    /**
     * Checks if the given processor is frozen and returns its state.
     *
     * @return true if the processor was frozen by calling the {@link #freeze()}
     * method.
     */
    public boolean isFrozen() {
        return frozen_;
    }

    /**
     * Abstract method that needs to be overriden in order to process a
     * document.
     *
     * @param document Document to process
     * @return Processed document or null if the document is filtered.
     * @throws IllegalStateException if processor is not frozen.
     */
    public final Annotation process(Annotation document) throws Exception {
        if (!isFrozen()) {
            throw new IllegalStateException("Processor should be frozen " +
                    "before the process method is called.");
        }
        return doProcess(document);
    }
}
