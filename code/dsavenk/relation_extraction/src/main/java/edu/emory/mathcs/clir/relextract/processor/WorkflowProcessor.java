package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.LinkedList;
import java.util.List;
import java.util.Properties;

/**
 * Processor, that combines several other processor into a single workflow.
 */
public class WorkflowProcessor extends Processor {

    // List of processors to apply.
    final private List<Processor> processors_ = new LinkedList<>();

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public WorkflowProcessor(Properties properties) {
        super(properties);
    }

    /**
     * Adds a processor to the internal list of processors.
     *
     * @param processor A processor to add.
     * @throws java.lang.IllegalStateException if the current processor is
     *                                         frozen.
     */
    public void addProcessor(Processor processor) throws IllegalStateException {
        if (isFrozen()) {
            throw new IllegalStateException("Cannot add processor to the " +
                    "frozen workflow.");
        }
        processors_.add(processor);
    }

    /**
     * Freezes the workflow by calling the {@link #freeze()} method for its
     * processors.
     *
     * @throws IllegalStateException if one of the processor is already frozen.
     */
    @Override
    public void freeze() throws IllegalStateException {
        for (Processor processor : processors_) {
            processor.freeze();
        }
        super.freeze();
    }

    /**
     * Processes the given document in a pipeline of processors.
     *
     * @param document A document to process.
     * @return Processed document or null if the document is filtered.
     */
    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws Exception {
        for (Processor processor : processors_) {
            document = processor.process(document);
            if (document == null) return null;
        }
        return document;
    }

    @Override
    public void finishProcessing() throws Exception {
        for (Processor processor : processors_) {
            processor.finishProcessing();
        }
    }
}
