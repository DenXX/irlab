package edu.emory.mathcs.ir.utilapps;

import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.spell.PlainTextDictionary;
import org.apache.lucene.search.spell.SpellChecker;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;

/**
 * Created by dsavenk on 8/26/15.
 */
public class BuildSpellCheckIndexApp {

    public static void main(String[] args) throws IOException {
        Directory dir =
                FSDirectory.open(FileSystems.getDefault().getPath(args[1]));
        SpellChecker spellChecker = new SpellChecker(dir);
        spellChecker.indexDictionary(
                new PlainTextDictionary(
                        FileSystems.getDefault().getPath(args[0])),
                new IndexWriterConfig(new KeywordAnalyzer()), true);
        spellChecker.close();
    }
}
