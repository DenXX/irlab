package edu.emory.mathcs.clir.relextract.tools;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.JaroWinklerDistance;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 10/29/14.
 */
public class FreebaseLexiconLuceneIndexBuilder {

    public static void main(String[] args) throws IOException {
        String lexiconFilename = args[0];
        String entityNamesFile = args[1];
        String namesIndexDir = args[2];
        String spellCheckIndexDir = args[3];

        indexEntityNames(namesIndexDir, lexiconFilename);
        buildSpellchekerIndex(entityNamesFile, spellCheckIndexDir);
    }

    private static void indexEntityNames(
            String namesIndexDir, String lexiconFilename) throws IOException {
        IndexWriter writer = new IndexWriter(
                FSDirectory.open(new File(namesIndexDir)),
                new IndexWriterConfig(Version.LATEST, new StandardAnalyzer(
                        CharArraySet.EMPTY_SET)));

        System.err.println("Indexing entity names ...");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(
                        new FileInputStream(lexiconFilename))));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            int langPos = fields[0].indexOf("\"@", 1);
            String name = fields[0].substring(1, langPos);
            String lang = fields[0].substring(langPos + 2);
            // Currently we only use English names and aliases.
            String bestEntity = "";
            long bestCount = 0;
            if (lang.equals("en")) {
                for (int i = 2; i < fields.length; i += 2) {
                    long count = Long.parseLong(fields[i]);
                    if (count > bestCount) {
                        bestCount = count;
                        bestEntity = fields[i - 1];
                    }
                }
                org.apache.lucene.document.Document doc =
                        new org.apache.lucene.document.Document();
                doc.add(new TextField("name", name, Field.Store.YES));
                doc.add(new StringField("id", bestEntity, Field.Store.YES));
                doc.add(new LongField("triple_count", bestCount,
                        Field.Store.YES));
                writer.addDocument(doc);

            }
        }
        writer.commit();
        writer.close();
    }

    private static void buildSpellchekerIndex(
            String entityNamesFile, String indexDir) throws IOException {
        System.err.println("Creating spell checker index...");
        File dir = new File(indexDir);
        Directory directory = FSDirectory.open(dir);
        // Creating spell-checker with Jaro-Winkler distance.
        SpellChecker spellChecker = new SpellChecker(directory,
                new JaroWinklerDistance());
        spellChecker.indexDictionary(
                new PlainTextDictionary(new File(entityNamesFile)),
                new IndexWriterConfig(Version.LATEST,
                        new StandardAnalyzer(new CharArraySet(0, true))),
                true);
        spellChecker.close();
    }
}
