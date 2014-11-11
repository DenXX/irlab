package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.Properties;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Sets a unique docid to every document in the collection.
 */
public class SetDocIdProcessor extends Processor {

    private AtomicInteger lastDocId = new AtomicInteger(0);

    public SetDocIdProcessor(Properties properties) {
        super(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        return document.toBuilder().setDocId(
                Integer.toString(lastDocId.getAndIncrement())).build();
    }
}
