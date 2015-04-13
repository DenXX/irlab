package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.AppParameters;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;

/**
 * Created by dsavenk on 9/26/14.
 */
public class TextInputProvider implements Iterable<Document.NlpDocument> {

    private final InputStream inputStream_;

    public TextInputProvider(Properties properties) throws FileNotFoundException {
        if (!properties.getProperty(AppParameters.INPUT_PARAMETER).equals("stdin"))
            inputStream_ = new FileInputStream(properties.getProperty(AppParameters.INPUT_PARAMETER));
        else
            inputStream_ = System.in;

    }

    @Override
    public Iterator<Document.NlpDocument> iterator() {
        try {
            return new TextInputIterator();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class TextInputIterator
            implements Iterator<Document.NlpDocument> {

        private final BufferedReader input_;
        private Document.NlpDocument currentObject_ = null;

        public TextInputIterator() throws IOException {
            input_ = new BufferedReader(
                    new InputStreamReader(inputStream_));
            readInputObject();
        }

        private void readInputObject() {
            try {
                String line;
                if ((line = input_.readLine()) != null) {
                    currentObject_ = Document.NlpDocument.newBuilder().setText(line).build();
                } else {
                    currentObject_ = null;
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
}