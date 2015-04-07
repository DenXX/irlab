package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by dsavenk on 4/7/15.
 */
public class BuildSearchIndexProcessor extends Processor {

    public static final String INDEX_LOCATION_PARAMETER = "index_store_path";

    public static final String QUESTION_FIELD_NAME = "question";
    public static final String QNA_FIELD_NAME = "qna";
    public static final String RELATIONS_FIELD_NAME = "relations";

    private final IndexWriter indexWriter_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public BuildSearchIndexProcessor(Properties properties) throws IOException {
        super(properties);
        String indexLocation = properties.getProperty(INDEX_LOCATION_PARAMETER);
        indexWriter_ = new IndexWriter(FSDirectory.open(new File(indexLocation)),
                new IndexWriterConfig(Version.LATEST, new StandardAnalyzer()));
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        org.apache.lucene.document.Document luceneDoc =
                new org.apache.lucene.document.Document();
        luceneDoc.add(new TextField(QUESTION_FIELD_NAME, document.getText().substring(0, document.getQuestionLength()), Field.Store.NO));
        luceneDoc.add(new StoredField(QNA_FIELD_NAME, document.getText()));
        document.getQaInstanceList().stream()
                .filter(Document.QaRelationInstance::getIsPositive)
                .forEach(x -> luceneDoc.add(new StringField(RELATIONS_FIELD_NAME, x.getPredicate(), Field.Store.YES)));
        indexWriter_.addDocument(luceneDoc);
        return document;
    }

    @Override
    public void finishProcessing() {
        try {
            indexWriter_.commit();
            indexWriter_.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
