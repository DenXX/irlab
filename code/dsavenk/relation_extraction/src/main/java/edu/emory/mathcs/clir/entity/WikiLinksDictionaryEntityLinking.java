package edu.emory.mathcs.clir.entity;

import edu.emory.mathcs.clir.relextract.extraction.Parameters;
import edu.emory.mathcs.clir.relextract.utils.NlpUtils;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 4/23/15.
 */
public class WikiLinksDictionaryEntityLinking implements EntityLinking {

    private final Map<String, Map<String, Float>> wikilinksDictionary_ = new HashMap<>();
    private final Map<String, Map<String, Float>> wikilinksLnrmDictionary_ = new HashMap<>();

    public WikiLinksDictionaryEntityLinking(String dictionaryPath, String lnrmDictionaryPath) throws IOException {
        if (dictionaryPath != null && lnrmDictionaryPath != null) {
            System.err.println("Starting reading wikilinks dictionary...");
            readDictionary(dictionaryPath, wikilinksDictionary_);
            System.err.println("Starting reading normalized wikilinks dictionary...");
            readDictionary(lnrmDictionaryPath, wikilinksLnrmDictionary_);
            System.err.println("Finished reading dictionaries.");
        } else {
            System.err.println("Dictionaries are not provided...");
        }
    }

    private void readDictionary(
            String dictFileName,
            Map<String, Map<String, Float>> dictionary) throws IOException {

        BufferedReader input = new BufferedReader(
                new InputStreamReader(
                        new GZIPInputStream(
                                new FileInputStream(dictFileName))));
        String line;
        String lastPhrase = null;
        Map<String, Float> scores = null;
        while ((line = input.readLine()) != null) {
            String[] fields = line.split("\t");
            if (!fields[0].equals(lastPhrase)) {
                if (scores == null || scores.isEmpty()) {
                    dictionary.remove(lastPhrase);
                }
                lastPhrase = fields[0];
                dictionary.put(lastPhrase, new HashMap<>());
                scores = dictionary.get(lastPhrase);
            }
            float score = Float.parseFloat(fields[2]);
            if (score >= Parameters.MIN_ENTITYID_SCORE) {
                scores.put(fields[1], score);
            }
        }
        // Put the last record to the dictionary.
        if (scores != null) {
            if (scores.isEmpty()) {
                dictionary.remove(lastPhrase);
            }
        }
    }

    @Override
    public Map<String, Float> resolveEntity(String name) {
        if (wikilinksDictionary_.containsKey(name)) return wikilinksDictionary_.get(name);
        String normalizedName = NlpUtils.normalizeStringForMatch(name);
        if (wikilinksLnrmDictionary_.containsKey(normalizedName)) return wikilinksLnrmDictionary_.get(normalizedName);
        return Collections.emptyMap();
    }
}
