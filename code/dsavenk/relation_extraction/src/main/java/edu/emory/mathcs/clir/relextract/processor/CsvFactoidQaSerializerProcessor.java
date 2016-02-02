package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.AppParameters;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.Properties;

import static org.apache.commons.lang.StringEscapeUtils.escapeHtml;

/**
 * Created by dsavenk on 2/1/16.
 */
public class CsvFactoidQaSerializerProcessor extends Processor {

    private final org.apache.commons.csv.CSVPrinter out_;
    private final String[] header_ = new String[]{"question", "answer",
            "question_entities", "answer_entities"};

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public CsvFactoidQaSerializerProcessor(Properties properties)
            throws IOException {
        super(properties);
        String outFilename = properties.getProperty(
                AppParameters.OUTPUT_PARAMETER);
        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator("\n");
        out_ = new CSVPrinter(new BufferedWriter(
                new OutputStreamWriter(
                        new FileOutputStream(outFilename))), csvFileFormat);
        out_.printRecord(header_);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document)
            throws IOException {
        synchronized (this) {
            DocumentWrapper doc = new DocumentWrapper(document);
            String[] record = new String[4];
            record[0] = doc.getQuestionText();
            record[1] = doc.getAnswerText();
            record[2] = escapeHtml(String.join("\t", doc.getQuestionAnswerEntities(true)));
            record[3] = escapeHtml(String.join("\t", doc.getQuestionAnswerEntities(false)));
            out_.printRecord(record);
        }
        return document;
    }

    @Override
    public void finishProcessing() throws Exception {
        out_.close();
    }
}
