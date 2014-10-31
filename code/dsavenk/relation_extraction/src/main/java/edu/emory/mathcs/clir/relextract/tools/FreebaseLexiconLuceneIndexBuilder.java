package edu.emory.mathcs.clir.relextract.tools;

import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.CharArraySet;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 10/29/14.
 */
public class FreebaseLexiconLuceneIndexBuilder {

    public static void main(String[] args) throws IOException {
        Properties props = new Properties();
        props.setProperty("kb", args[3]);
        KnowledgeBase kb = KnowledgeBase.getInstance(props);

        File indexDir = new File(args[4]);
        IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir),
                new IndexWriterConfig(Version.LATEST, new StandardAnalyzer(
                        new CharArraySet(0, true))));

        Map<String, Long> entityTripleCount = new HashMap<>();

        String lexicon_filename = args[0];
        String wikilinks_filename = args[1];
        String entityNamesFile = args[2];
        System.err.println("Indexing entity names ...");
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(new GZIPInputStream(
                        new FileInputStream(lexicon_filename))));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            int langPos = fields[0].indexOf("\"@", 1);
            String name = fields[0].substring(1, langPos);
            String lang = fields[0].substring(langPos + 2);
            // Currently we only use English names and aliases.
            if (lang.equals("en")) {
                long maxCount = 0;
                for (int i = 2; i < fields.length; i += 2) {
                    long count = Long.parseLong(fields[i]);
                    if (count > maxCount) {
                        maxCount = count;
                        org.apache.lucene.document.Document doc =
                                new org.apache.lucene.document.Document();
                        doc.add(new TextField("name", name, Field.Store.YES));
                        doc.add(new StringField("id", fields[i - 1], Field.Store.YES));
                        doc.add(new LongField("triple_count", count, Field.Store.YES));
                        doc.add(new LongField("phrase_count", -1, Field.Store.YES));
                        if (!entityTripleCount.containsKey(fields[i - 1])) {
                            entityTripleCount.put(fields[i - 1], count);
                        }
                        writer.addDocument(doc);
                    }
                }
            }
        }
        System.err.println("Reading Wikilinks file");
        reader = new BufferedReader(new InputStreamReader(
                new FileInputStream(wikilinks_filename)));

        String prevName = "";
        String prevMid = "";
        int count = 0;
        while ((line = reader.readLine()) != null) {
            String[] fields = line.split("\t");
            String name = normalizeEntityName(fields[0]);
            String mid = fields[1];
            // If this is just another mention of the same entity with the same
            // anchor text we continue counting.
            if (name.equals(prevName) && mid.equals(prevMid)) {
                ++count;
                continue;
            }

            if (count > 0) {
                org.apache.lucene.document.Document doc =
                        new org.apache.lucene.document.Document();
                doc.add(new TextField("name", prevName, Field.Store.YES));
                doc.add(new StringField("id", prevMid, Field.Store.YES));
                if (!entityTripleCount.containsKey(prevMid)) {
                    long tripleCount = kb.getTripleCount(prevMid);
                    entityTripleCount.put(prevMid, tripleCount);
                }
                doc.add(new LongField("triple_count",
                        entityTripleCount.get(prevMid), Field.Store.YES));
                doc.add(new LongField("phrase_count", count, Field.Store.YES));
                writer.addDocument(doc);
            }

            prevName = name;
            prevMid = mid;
            count = 1;
        }
        if (count > 0) {
            org.apache.lucene.document.Document doc =
                    new org.apache.lucene.document.Document();
            doc.add(new TextField("name", prevName, Field.Store.YES));
            doc.add(new StringField("id", prevMid, Field.Store.YES));
            if (!entityTripleCount.containsKey(prevMid)) {
                long tripleCount = kb.getTripleCount(prevMid);
                entityTripleCount.put(prevMid, tripleCount);
            }
            doc.add(new LongField("triple_count",
                    entityTripleCount.get(prevMid), Field.Store.YES));
            doc.add(new LongField("phrase_count", count, Field.Store.YES));
            writer.addDocument(doc);

        }

        System.err.println("Done indexing entity names.");
        writer.commit();
        writer.close();

        System.err.println("Creating spell checker index...");
        File dir = new File(args[5]);
        Directory directory = FSDirectory.open(dir);
        SpellChecker spellChecker = new SpellChecker(directory);
        spellChecker.indexDictionary(
                new PlainTextDictionary(new File(entityNamesFile)), new IndexWriterConfig(Version.LATEST,
                        new StandardAnalyzer(new CharArraySet(0, true))),
                true);
        spellChecker.close();
    }

    private static String normalizeEntityName(String name) {
        int startIndex = 0;
        while (startIndex < name.length() &&
                !Character.isLetterOrDigit(name.charAt(startIndex))) {
            ++startIndex;
        }

        int endIndex = name.length() - 1;
        while (endIndex >= 0 &&
                !Character.isLetterOrDigit(name.charAt(endIndex))) {
            --endIndex;
        }

        if (startIndex < name.length() && endIndex >= 0 &&
                startIndex <= endIndex) {
            return name.substring(startIndex, endIndex + 1);
        }
        return "";
    }

}
