package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Created by dsavenk on 4/7/15.
 */
public class BuildSearchIndexProcessor extends Processor {

    public static final String INDEX_LOCATION_PARAMETER = "index_store_path";

    public static final String QUESTION_FIELD_NAME = "question";
    public static final String QUESTION_TEMPLATE_FIELD_NAME = "question_template";
    public static final String QUESTION_FEATURES_FIELD_NAME = "question_features";
    public static final String QNA_FIELD_NAME = "qna";
    public static final String RELATIONS_FIELD_NAME = "relations";

    private static KnowledgeBase kb_;

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
        Map<String, Analyzer> analyzers = new HashMap<>();
        analyzers.put(RELATIONS_FIELD_NAME, new KeywordAnalyzer());
        analyzers.put(QUESTION_FIELD_NAME, new EnglishAnalyzer(CharArraySet.EMPTY_SET));
        analyzers.put(QUESTION_TEMPLATE_FIELD_NAME, new SimpleAnalyzer());
        analyzers.put(QUESTION_FEATURES_FIELD_NAME, new KeywordAnalyzer());
        indexWriter_ = new IndexWriter(FSDirectory.open(new File(indexLocation)),
                new IndexWriterConfig(Version.LATEST,
                        new PerFieldAnalyzerWrapper(new SimpleAnalyzer(),
                                analyzers)));
        kb_ = KnowledgeBase.getInstance(properties);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        DocumentWrapper docWrapper = new DocumentWrapper(document);

        org.apache.lucene.document.Document luceneDoc =
                new org.apache.lucene.document.Document();
        luceneDoc.add(new TextField(QUESTION_FIELD_NAME, document.getText().substring(0, document.getQuestionLength()), Field.Store.NO));
        luceneDoc.add(new StoredField(QNA_FIELD_NAME, document.getText()));
        document.getQaInstanceList().stream()
                .filter(Document.QaRelationInstance::getIsPositive)
                .forEach(x -> luceneDoc.add(new StringField(RELATIONS_FIELD_NAME, x.getPredicate(), Field.Store.YES)));

        for (Document.Span span : document.getSpanList()) {
            if (!span.getType().equals("MEASURE")) {
                int index = 0;
                for (Document.Mention mention : span.getMentionList()) {
                    if (mention.getSentenceIndex() < docWrapper.getQuestionSentenceCount()) {
                        String template = NlpUtils.getQuestionTemplate(document, mention.getSentenceIndex(), span, index)
                                .replace("[", "").replace("]", "").replace("<", "").replace(">", "").replace("?", "");
                        luceneDoc.add(new TextField(QUESTION_TEMPLATE_FIELD_NAME, template, Field.Store.YES));
                        break;
                    }
                    ++index;
                }
            }
        }

        for (int questionSentenceIndex = 0; questionSentenceIndex < docWrapper.getQuestionSentenceCount(); ++questionSentenceIndex) {
            for (String feat : new QAModelTrainerProcessor.QuestionGraph(docWrapper, kb_, questionSentenceIndex, false).getEdgeFeatures()) {
                luceneDoc.add(new StringField(QUESTION_FEATURES_FIELD_NAME, feat, Field.Store.YES));
            }
        }

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
