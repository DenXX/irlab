package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.AppParameters;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 9/26/14.
 */
public class DeserializerInputProvider implements Iterable<Document.NlpDocument> {

    public DeserializerInputProvider(Properties properties) {
        inputFilename_ = properties.getProperty(AppParameters.INPUT_PARAMETER);
    }

    @Override
    public Iterator<Document.NlpDocument> iterator() {
        try {
            return new DeserializerInputIterator();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class DeserializerInputIterator
            implements Iterator<Document.NlpDocument> {

        private final InputStream input_;
        private Document.NlpDocument currentObject_ = null;

        public DeserializerInputIterator() throws IOException {
            input_ = new BufferedInputStream(
                    new GZIPInputStream(
                            new FileInputStream(inputFilename_)));
            readInputObject();
        }

        private void readInputObject() {
            try {
                currentObject_ = Document.NlpDocument.parseDelimitedFrom(input_);
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

    private final String inputFilename_;
}