package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by dsavenk on 10/17/14.
 */
public class TestProcessor extends Processor {
    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public TestProcessor(Properties properties) throws IOException {
        super(properties);

        File dir = new File("/home/dsavenk/ir/data/Freebase/entity_names_index/");
        Directory directory = FSDirectory.open(dir);
        SpellChecker spellCherker = new SpellChecker(directory);
        StandardAnalyzer analyzer = new StandardAnalyzer(Version.LUCENE_36);
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_36, analyzer);
        spellCherker.indexDictionary(
                new PlainTextDictionary(
                        new File("/home/dsavenk/ir/data/Freebase/entity_names.txt")),
                config, true);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        if (document.getRelationCount() == 0) {
            int count = 0;
            for (Document.Span span : document.getSpanList()) {
                if (span.hasEntityId()) {
                    ++count;
                }
            }
            if (count >= 2) {
                System.out.println("-------------------------------------");
                System.out.println(document.getText());
                System.out.println("===");
                for (Document.Span span : document.getSpanList()) {
                    if (span.hasEntityId()) {
                        System.out.println(span.getText() + " [" + span.getEntityId() + "]");
                    }
                }
            }
        }
        return null;
    }
}
