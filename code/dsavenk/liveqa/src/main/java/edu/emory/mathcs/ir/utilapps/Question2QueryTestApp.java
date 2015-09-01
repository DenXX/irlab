package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.query.QueryFormulation;
import edu.emory.mathcs.ir.qa.answerer.query.TopIdfTermsQueryFormulator;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;

/**
 * Created by dsavenk on 8/26/15.
 */
public class Question2QueryTestApp {
    public static void main(String[] args) throws IOException {
        final BufferedReader input = new BufferedReader(
                new InputStreamReader(System.in));
        final IndexReader indexReader =
                DirectoryReader.open(FSDirectory.open(
                        FileSystems.getDefault().getPath(args[0])));
        QueryFormulation queryFormulation =
                new TopIdfTermsQueryFormulator(indexReader, false, 5);

        String line;
        while ((line = input.readLine()) != null) {
            final String[] lineFields = line.split("\t");
            final Question question =
                    new Question("", lineFields[0],
                            lineFields.length > 1 ? lineFields[1] : "", "");
            System.out.println(queryFormulation.getQuery(question));
        }
    }
}
