package edu.emory.mathcs.clir.relextract.data;

import edu.emory.mathcs.clir.relextract.AppParameters;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.Properties;

/**
 * Created by dsavenk on 9/26/14.
 */
public class MultipleFilesInputProvider<InputProvider extends Iterable<Document.NlpDocument>> implements Iterable<Document.NlpDocument> {

    public static final String NAMES_SEPARATOR = ",";

    private final String[] inputFilenames_;
    private int currentFilename_;
    private InputProvider currentProvider_;
    private Iterator<Document.NlpDocument> currentIterator_;
    private Class<InputProvider> providerClass_;
    private Properties props_;

    public MultipleFilesInputProvider(Properties properties, Class<InputProvider> cls) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        inputFilenames_ = properties.getProperty(AppParameters.INPUT_PARAMETER).split(NAMES_SEPARATOR);
        currentFilename_ = -1;
        providerClass_ = cls;
        props_ = (Properties)properties.clone();
        createNextProviderIterator();
    }

    private void createNextProviderIterator() throws InstantiationException, IllegalAccessException, InvocationTargetException, NoSuchMethodException {
        if (++currentFilename_ < inputFilenames_.length) {
            props_.setProperty(AppParameters.INPUT_PARAMETER, inputFilenames_[currentFilename_]);
            currentProvider_ = providerClass_.getConstructor(props_.getClass()).newInstance(props_);
            currentIterator_ = currentProvider_.iterator();
        } else {
            currentProvider_ = null;
            currentIterator_ = null;
        }
    }

    @Override
    public Iterator<Document.NlpDocument> iterator() {
        try {
            return new MultipleFilesInputProviderIterator();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public class MultipleFilesInputProviderIterator
            implements Iterator<Document.NlpDocument> {

        private Document.NlpDocument currentObject_ = null;

        public MultipleFilesInputProviderIterator() throws IOException {
            readNextObject();
        }

        private void readNextObject() {
            currentObject_ = null;
            while (currentObject_ == null && currentFilename_ < inputFilenames_.length) {
                if (currentIterator_ != null && currentIterator_.hasNext()) {
                    currentObject_ = currentIterator_.next();
                } else {
                    try {
                        createNextProviderIterator();
                    } catch (InstantiationException | NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                        e.printStackTrace();
                    }
                }
            }
        }


        @Override
        public boolean hasNext() {
            return currentObject_ != null;
        }

        @Override
        public Document.NlpDocument next() {
            Document.NlpDocument obj = currentObject_;
            readNextObject();
            return obj;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("Iterator is read-only!");
        }
    }
}