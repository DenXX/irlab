package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.input.YahooAnswersXmlInput;
import edu.emory.mathcs.ir.qa.AppConfig;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

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

            // Get the list of Yahoo! Answers categories selected for LiveQA.
            Set<String> categories = Arrays.stream(
                    AppConfig.PROPERTIES.getProperty(
                            AppConfig.LIVEQA_CATEGORIES_PARAMETER).split(";"))
                    .collect(Collectors.toSet());

            int counter = 0;
            int skipped = 0;
            for (YahooAnswersXmlInput.QnAPair qna : input) {
                // Check if question is written in English and that the
                // category of the question belongs to the list of categories
                // selected for LiveQA task.
                if (qna.attributes.containsKey("qlang") &&
                        qna.attributes.get("qlang").equals("en") &&
                        categories.contains(qna.categories[0])) {
                    // Convert QnA document to Lucene document format.
                    final Document indexDocument =
                            QnAIndexDocument.getIndexDocument(qna);
                    indexWriter.addDocument(indexDocument);

                    // Add friendly output to keep us updated on the progress.
                    if (++counter % 10000 == 0) {
                        System.err.println(String.format(
                                "%d documents processed and %d skipped",
                                counter, skipped));
                    }
                } else {
                    ++skipped;
                }
            }
            Finalize(indexWriter);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
