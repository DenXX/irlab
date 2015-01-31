package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.AppParameters;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 9/26/14.
 */
public class DeserializerBatchInputProvider implements Iterable<Document.NlpDocument> {

    public DeserializerBatchInputProvider(Properties properties) {
        inputName_ = properties.getProperty(AppParameters.INPUT_PARAMETER);
    }

    @Override
    public Iterator<Document.NlpDocument> iterator() {
        try {
            return new DeserializerBatchInputIterator();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class DeserializerBatchInputIterator implements Iterator<Document.NlpDocument> {

        private int currentBatch_;
        private InputStream input_;
        private Document.NlpDocument currentObject_ = null;

        public DeserializerBatchInputIterator() throws IOException {
            currentBatch_ = 0;
            if (openNextInputStream())
                readInputObject();
        }

        private boolean openNextInputStream() throws IOException {
            if (input_ != null) input_.close();
            String nextBatchName = inputName_ + "_" + currentBatch_;
            if (!new File(nextBatchName).exists()) return false;
            input_ = new BufferedInputStream(
                    new GZIPInputStream(new FileInputStream(nextBatchName)));
            ++currentBatch_;
            return true;
        }

        private void readInputObject() {
            try {
                try {
                    currentObject_ = Document.NlpDocument.parseDelimitedFrom(input_);
                } finally {
                    if (currentObject_ == null) {
                        if (openNextInputStream()) {
                            readInputObject();
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                currentObject_ = null;
            }
        }

        @Override
        public boolean hasNext() {
            return currentObject_ != null;
        }

        @Override
        public Document.NlpDocument next() {
            Document.NlpDocument obj = currentObject_;
            readInputObject();
            return obj;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator is read-only!");
        }
    }

    private final String inputName_;
}