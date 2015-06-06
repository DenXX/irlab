package edu.emory.mathcs.clir.document;

import com.google.common.collect.ClassToInstanceMap;
import com.google.common.collect.MutableClassToInstanceMap;

/**
 * Represents a document with a set of annotations. Annotations are stored in typesafe heterogeneous container.
 */
public class Document {
    private String text_;
    private ClassToInstanceMap<Annotation> annotations_ = MutableClassToInstanceMap.create();

    public Document(String text) {
        text_ = text;
    }

    /**
     * Returns text of the document.
     * @return Text of the document as a string.
     */
    public String getText() {
        return text_;
    }
}
