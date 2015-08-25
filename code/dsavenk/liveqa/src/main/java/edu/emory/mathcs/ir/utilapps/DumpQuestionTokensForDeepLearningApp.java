package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.input.YahooAnswersXmlInput;
import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.index.QnAIndexDocument;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.answerer.query.TitleNoStopwordsQueryFormulator;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileSystems;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 8/25/15.
 */
public class DumpQuestionTokensForDeepLearningApp {
    public static final int TOPN = 10;

    public static void main(String[] args) {
        final String indexLocation = args[0];
        try {
            PrintWriter out = new PrintWriter(
                    new BufferedOutputStream(new FileOutputStream(args[1])));

            QueryFormulation queryFormulator =
                    new TitleNoStopwordsQueryFormulator();
            final Directory directory;
            directory = FSDirectory.open(
                    FileSystems.getDefault().getPath(indexLocation));
            final IndexReader indexReader = DirectoryReader.open(directory);
            final IndexSearcher searcher = new IndexSearcher(indexReader);

            for (int docid = 0; docid < indexReader.maxDoc(); ++docid) {
                final YahooAnswersXmlInput.QnAPair qna =
                        QnAIndexDocument.getQnAPair(
                                indexReader.document(docid));
                printQnA(true, qna.getQuestion(), qna.getAnswer(), out);
                try {
                    // Get similar QnA pairs.
                    final YahooAnswersXmlInput.QnAPair[] similarQnAPairs =
                            QnAIndexDocument.getSimilarQnAPairs(
                                    searcher, qna, queryFormulator, TOPN);
                    for (final YahooAnswersXmlInput.QnAPair similarQna :
                            similarQnAPairs) {
                        printQnA(false, qna.getQuestion(),
                                similarQna.getAnswer(), out);
                    }

                } catch (Exception e) {
                    System.err.println(e.getMessage());
                }

                if (docid % 1000 == 0) {
                    System.err.println(
                            String.format("%d qna processed", docid));
                }
                //if (docid > 100) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printQnA(boolean b, Question question, Answer answer,
                                 PrintWriter out) {
        StringBuilder line = new StringBuilder();
        line.append(b ? "1" : "0");
        line.append("\t");
        line.append(question.getTitle().getLemmaList(false).stream()
                .collect(Collectors.joining(" "))
                .replace("\t", " ").replace("\n", " "));
        line.append("\t");
        line.append(question.getBody().getLemmaList(false).stream()
                .collect(Collectors.joining(" "))
                .replace("\t", " ").replace("\n", " "));
        line.append("\t");
        line.append(answer.getAnswer().getLemmaList(false).stream()
                .collect(Collectors.joining(" "))
                .replace("\t", " ").replace("\n", " "));
        out.println(line);
    }
}
