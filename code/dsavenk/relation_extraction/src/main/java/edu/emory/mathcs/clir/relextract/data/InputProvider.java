package edu.emory.mathcs.clir.relextract.data;

import edu.stanford.nlp.pipeline.Annotation;

import java.util.Iterator;

/**
 * Abstract class that represents an iterator over a set of annotations.
 * Different input providers will use different ways to read/generate inputs.
 */
public abstract class InputProvider implements Iterator<Annotation> {

    /**
     * InputProviders are read-only.
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException(
                "Cannot remove from InputProvider");
    }
}
