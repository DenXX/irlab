package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;

import java.io.IOException;
import java.util.Properties;

/**
 * Created by dsavenk on 10/17/14.
 */
public class TestProcessor extends Processor {
    private int count = 0;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public TestProcessor(Properties properties) throws IOException {
        super(properties);

//        Analyzer analyzer = new StandardAnalyzer(new CharArraySet(0, true));
//        IndexWriterConfig config = new IndexWriterConfig(Version.LATEST,
//                analyzer);
//        File indexDir = new File("/home/dsavenk/ir/data/Freebase/lexicon_index/");
//        IndexWriter writer = new IndexWriter(FSDirectory.open(indexDir),
//                config);
//
//
//        String lexicon_filename = properties.getProperty(EntityResolutionProcessor.LEXICON_PARAMETER);
//        System.err.println("Indexing entity names ...");
//        BufferedReader reader = new BufferedReader(
//                new InputStreamReader(new GZIPInputStream(
//                        new FileInputStream(lexicon_filename))));
//        String line;
//        while ((line = reader.readLine()) != null) {
//            String[] fields = line.split("\t");
//            int langPos = fields[0].indexOf("\"@", 1);
//            String name = fields[0].substring(1, langPos);
//            String lang = fields[0].substring(langPos + 2);
//            // Currently we only use English names and aliases.
//            if (lang.equals("en")) {
//                long maxCount = 0;
//                String maxCountEntity = "";
//                for (int i = 2; i < fields.length; i += 2) {
//                    long count = Long.parseLong(fields[i]);
//                    if (count > maxCount) {
//                        maxCount = count;
//                        maxCountEntity = fields[i-1];
//                        org.apache.lucene.document.Document doc =
//                                new org.apache.lucene.document.Document();
//                        doc.add(new TextField("name", name, Field.Store.YES));
//                        doc.add(new StringField("id", fields[i-1], Field.Store.YES));
//                        doc.add(new LongField("triple_count", count, Field.Store.YES));
//                        writer.addDocument(doc);
//                    }
//                }
//            }
//        }
//        System.err.println("Done indexing entity names.");
//        writer.commit();
//        writer.close();

//        System.err.println("Creating spell checker index...");
//        File dir = new File("/home/dsavenk/ir/data/Freebase/spellcheck_index/");
//        Directory directory = FSDirectory.open(dir);
//        SpellChecker spellCherker = new SpellChecker(directory);
//        spellCherker.indexDictionary(
//                new LuceneDictionary(
//                        DirectoryReader.open(FSDirectory.open(indexDir)),
//                        "name"), config, true);
//        spellCherker.close();
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        System.out.println(++count);
//        for (Document.Span span : document.getSpanList()) {
//            if (span.getType().equals("ENTITY")) {
//                System.out.println(span.getValue() + "\t" + (span.hasEntityId() ? span.getEntityId() : ""));
//            }
//        }
//        if (document.getRelationCount() == 0) {
//            int count = 0;
//            for (Document.Span span : document.getSpanList()) {
//                if (span.hasEntityId()) {
//                    ++count;
//                }
//            }
//            if (count >= 2) {
//                System.out.println("-------------------------------------");
//                System.out.println(document.getText());
//                System.out.println("===");
//                for (Document.Span span : document.getSpanList()) {
//                    if (span.hasEntityId()) {
//                        System.out.println(span.getText() + " [" + span.getEntityId() + "]");
//                    }
//                }
//            }
//        }
        return null;
    }
}
