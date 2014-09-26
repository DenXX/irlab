package edu.emory.mathcs.clir.relextract.processor;

import edu.stanford.nlp.pipeline.Annotation;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Properties;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dsavenk on 9/26/14.
 */
public class SerializerProcessor extends Processor {
    private final ObjectOutputStream out_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public SerializerProcessor(Properties properties) throws IOException {
        super(properties);
        String outFilename = properties.getProperty("output");
        out_ = new ObjectOutputStream(new BufferedOutputStream(
                new GZIPOutputStream(new FileOutputStream(outFilename))));
    }

    @Override
    protected Annotation doProcess(Annotation document) throws IOException {
        out_.writeObject(document);
        return document;
    }
}
