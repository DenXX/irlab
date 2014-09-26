package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.data.InputProvider;
import edu.emory.mathcs.clir.relextract.processor.Processor;
import edu.stanford.nlp.pipeline.Annotation;

import java.util.Properties;

/**
 * Object of this class allows one to process a collection of documents with
 * some processor.
 */
public class SequentialProcessingRunner {

    private final Processor processor_;

    public SequentialProcessingRunner(Processor processor,
                                      Properties properties) {
        processor_ = processor;
        if (!processor.isFrozen()) {
            processor.freeze();
        }
    }

    /**
     * Runs a processor over the provided collection of documents.
     *
     * @param documents A collections of documents to process.
     * @throws InterruptedException
     */
    public void run(InputProvider documents) throws Exception {
        final long startTime = System.currentTimeMillis();
        int counter = 0;
        int filtered = 0;
        while (documents.hasNext()) {
            final Annotation document = documents.next();
            // Parser reads the next input on demand, that's why it cannot
            // predict that the input is over, so it will actually return null
            // element.
            if (document == null) continue;
            if (processor_.process(document) == null) {
                ++filtered;
            }
            if (++counter % 50 == 0) {
                long currentTime = System.currentTimeMillis();
                System.err.println("Processed: " + counter +
                        " (" + (1000.0 * counter /
                        (currentTime - startTime)) + " docs/sec");
                System.err.println("Filtered: " + filtered);
            }
            if (counter > 1000) break;
        }
    }
}
