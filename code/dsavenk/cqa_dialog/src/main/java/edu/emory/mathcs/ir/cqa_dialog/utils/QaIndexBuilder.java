package edu.emory.mathcs.ir.cqa_dialog.utils;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 1/15/16.
 */
public class QaIndexBuilder {

    private static final String ID_FIELD_NAME = "id";
    private static final String QTITLE_FIELD_NAME = "title";
    private static final String QBODY_FIELD_NAME = "body";
    private static final String QTITLEBODY_FIELD_NAME = "title_body";
    private static final String QCOMMENT_NAME = "question_comment";

    private static void Finalize(IndexWriter indexWriter) throws IOException {
        indexWriter.forceMerge(1);
        indexWriter.commit();
        indexWriter.close();
    }

    public static IndexWriter createIndexWriter(Directory indexLocation)
            throws IOException {
        Map<String, Analyzer> analyzers = new HashMap<>();
        analyzers.put(ID_FIELD_NAME, new KeywordAnalyzer());
        analyzers.put(QTITLE_FIELD_NAME, new EnglishAnalyzer());
        analyzers.put(QBODY_FIELD_NAME, new EnglishAnalyzer());
        analyzers.put(QTITLEBODY_FIELD_NAME, new EnglishAnalyzer());
        analyzers.put(QCOMMENT_NAME, new EnglishAnalyzer());

        final IndexWriterConfig config =
                new IndexWriterConfig(
                        new PerFieldAnalyzerWrapper(new SimpleAnalyzer(),
                                analyzers));
        return new IndexWriter(indexLocation, config);
    }


    public static Document getIndexDocument(int id,
                                            String title,
                                            String body,
                                            String comment) {
        final String titleAndBody = String.format("%s\n%s", title, body);
        final Document indexDocument = new Document();
        indexDocument.add(new StoredField(ID_FIELD_NAME, id));
        indexDocument.add(new TextField(QTITLE_FIELD_NAME,
                title, Field.Store.YES));
        indexDocument.add(new TextField(
                QBODY_FIELD_NAME, body, Field.Store.YES));
        indexDocument.add(new TextField(QTITLEBODY_FIELD_NAME,
                titleAndBody, Field.Store.YES));
        indexDocument.add(new TextField(QCOMMENT_NAME, comment,
                Field.Store.YES));
        return indexDocument;
    }

    public static void main(String[] args) {
        IndexWriter indexWriter;
        try {
            final Directory directory = FSDirectory.open(
                    FileSystems.getDefault().getPath(args[1]));
            indexWriter = createIndexWriter(directory);

            int counter = 0;
            Scanner in = new Scanner(
                    new BufferedInputStream(new FileInputStream(args[0])));
            while (in.hasNext()) {
                in.nextLine();
                int id = in.nextInt();
                in.nextLine();
                String title = in.nextLine().replace("QT: ", "").replace("<p>", "").replace("</p>", "");
                String body = in.nextLine().replace("QB: ", "").replace("<p>", "").replace("</p>", "");;
                String comment = in.nextLine().replace("CC: ", "").replace("<p>", "").replace("</p>", "");
                in.nextLine();
                in.nextLine();
                final Document indexDocument =
                        getIndexDocument(id, title, body, comment);
                indexWriter.addDocument(indexDocument);

                // Add friendly output to keep us updated on the progress.
                if (++counter % 1000 == 0) {
                    System.err.println(String.format(
                            "%d documents processed", counter));
                }
            }
            Finalize(indexWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
