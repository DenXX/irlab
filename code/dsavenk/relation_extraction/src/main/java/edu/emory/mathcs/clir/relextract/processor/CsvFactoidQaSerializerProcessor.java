package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.AppParameters;
import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
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
    private final String[] header_ = new String[]{"id", "question", "answer",
            "question_entities", "answer_entities"};
    private final KnowledgeBase kb_;

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
        kb_ = KnowledgeBase.getInstance(properties);
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
            String id = document.getDocId();
            for (Document.Attribute attr : document.getAttributeList()) {
                if (attr.getKey().equals("id")) {
                    id = attr.getValue();
                }
            }
            String[] record = new String[] {
                    id,
                    doc.getQuestionText(),
                    doc.getAnswerText(),
                    escapeHtml(String.join("\t", doc.getQuestionAnswerEntities(true, kb_))),
                    escapeHtml(String.join("\t", doc.getQuestionAnswerEntities(false, kb_)))
            };
            out_.printRecord(record);
        }
        return document;
    }

    @Override
    public void finishProcessing() throws Exception {
        out_.close();
    }
}
