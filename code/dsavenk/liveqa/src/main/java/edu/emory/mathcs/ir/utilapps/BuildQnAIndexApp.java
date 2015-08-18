package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.input.YahooAnswersXmlInput;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.*;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by dsavenk on 8/18/15.
 */
public class BuildQnAIndexApp {

    private static void Finalize(IndexWriter indexWriter) throws IOException {
        indexWriter.forceMerge(1);
        indexWriter.commit();
        indexWriter.close();
    }

    public static void main(String[] args) {
        IndexWriter indexWriter;
        try {
            final Directory directory = FSDirectory.open(
                    FileSystems.getDefault().getPath(args[1]));
            indexWriter = QnAIndexDocument.createIndexWriter(directory);
            YahooAnswersXmlInput input = new YahooAnswersXmlInput(args[0]);
            int counter = 0;
            for (YahooAnswersXmlInput.QnAPair qna : input) {
                if (qna.attributes.containsKey("qlang") &&
                        qna.attributes.get("qlang").equals("en")) {

                    // Convert QnA document to Lucene document format.
                    final Document indexDocument =
                            QnAIndexDocument.getIndexDocument(qna);
                    indexWriter.addDocument(indexDocument);

                    // Add friendly output to keep us updated on the progress.
                    if (++counter % 10000 == 0) {
                        System.err.println(String.format(
                                "%d documents processed", counter));
                    }
                }
            }
            Finalize(indexWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
