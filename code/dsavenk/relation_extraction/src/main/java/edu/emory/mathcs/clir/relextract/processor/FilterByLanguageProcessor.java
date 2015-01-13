package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.util.Properties;

/**
 * Created by dsavenk on 1/12/15.
 */
public class FilterByLanguageProcessor extends Processor {

    public static final String LANGUAGE_FILTER_PARAMETER = "lang";

    public static final String DOCUMENT_LANGUAGE_ATTRIBUTE = "qlang";

    private String lang2keep_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public FilterByLanguageProcessor(Properties properties) {
        super(properties);
        lang2keep_ = properties.getProperty(LANGUAGE_FILTER_PARAMETER);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        for (Document.Attribute attr : document.getAttributeList()) {
            if (attr.getKey().equals(DOCUMENT_LANGUAGE_ATTRIBUTE)) {
                if (attr.getValue().equals(lang2keep_)) return document;
            }
        }
        return null;
    }
}
