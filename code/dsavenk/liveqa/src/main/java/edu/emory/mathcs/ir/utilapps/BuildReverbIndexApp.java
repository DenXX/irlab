package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.qa.ml.ReverbTriplesFeatureGenerator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.SimpleAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.bouncycastle.util.Strings;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 8/21/15.
 */
public class BuildReverbIndexApp {
    public static void main(String[] args) {
        IndexWriter indexWriter;
        try {
            final Directory directory = FSDirectory.open(
                    FileSystems.getDefault().getPath(args[1]));
            indexWriter = createIndexWriter(directory);

            final BufferedReader input =
                    new BufferedReader(
                            new InputStreamReader(
                                    new GZIPInputStream(
                                            new FileInputStream(args[0]))));
            String line;
            while ((line = input.readLine()) != null) {
                String[] fields = Strings.split(line, '\t');
                Document doc = new Document();
                doc.add(new TextField(
                        ReverbTriplesFeatureGenerator.SUBJECT_FIELDNAME,
                        fields[4], Field.Store.YES));
                doc.add(new TextField(
                        ReverbTriplesFeatureGenerator.PREDICATE_FIELDNAME,
                        fields[5], Field.Store.YES));
                doc.add(new TextField(
                        ReverbTriplesFeatureGenerator.OBJECT_FIELDNAME,
                        fields[6], Field.Store.YES));
                indexWriter.addDocument(doc);
            }

            indexWriter.forceMerge(1);
            indexWriter.commit();
            indexWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static IndexWriter createIndexWriter(Directory directory)
            throws IOException {
        Map<String, Analyzer> analyzers = new HashMap<>();
        analyzers.put(ReverbTriplesFeatureGenerator.SUBJECT_FIELDNAME,
                new EnglishAnalyzer());
        analyzers.put(ReverbTriplesFeatureGenerator.OBJECT_FIELDNAME,
                new EnglishAnalyzer());
        analyzers.put(ReverbTriplesFeatureGenerator.PREDICATE_FIELDNAME,
                new EnglishAnalyzer());
        final IndexWriterConfig config =
                new IndexWriterConfig(
                        new PerFieldAnalyzerWrapper(new SimpleAnalyzer(),
                                analyzers));
        return new IndexWriter(directory, config);
    }
}
