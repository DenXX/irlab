package edu.emory.mathcs.clir.relextract.tools;

import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dsavenk on 11/10/14.
 */
public class ProcessCrossWikisDictionary {

    public static void main(String[] args) {
        try {
            Map<String, String> wiki2Freebase = readWiki2Freebase(args[0]);
            BufferedReader input = new BufferedReader(
                    new InputStreamReader(
                            new BZip2CompressorInputStream(
                                    new FileInputStream(args[1]))));
            PrintWriter out = new PrintWriter(
                    new BufferedWriter(
                            new OutputStreamWriter(
                                    new GZIPOutputStream(
                                            new FileOutputStream(args[2])))));

            String line;
            while ((line = input.readLine()) != null) {
                String[] fields = line.split("\t");
                String source = fields[0];
                String[] scoreWiki = fields[1].split(" ");
                float score = Float.parseFloat(scoreWiki[0]);
                String wiki = scoreWiki[1];
                if (wiki2Freebase.containsKey(wiki)) {
                    out.println(source + "\t" + wiki2Freebase.get(wiki) + "\t" + score);
                }
            }
            input.close();
            out.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }

    private static Map<String, String> readWiki2Freebase(String dictFile) throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(dictFile));
        Map<String, String> wiki2Freebase = new HashMap<>();
        String line;
        while ((line = input.readLine()) != null) {
            String[] fields = line.split("\t");
            wiki2Freebase.put(fields[0], fields[1]);
        }
        input.close();
        return wiki2Freebase;
    }

}