package edu.emory.mathcs.clir.relextract.tools;

import edu.emory.mathcs.clir.relextract.utils.NlpUtils;

import java.io.*;

/**
 * Created by dsavenk on 11/10/14.
 */
public class ProcessWikipedia2Freebase {

    public static void main(String[] args) {
        try {
            BufferedReader input = new BufferedReader(new FileReader(args[0]));
            PrintWriter out =
                    new PrintWriter(new BufferedWriter(new FileWriter(args[1])));
            String line;
            while ((line = input.readLine()) != null) {
                String[] midTitle = line.split("\t");
                String mid = "/" + midTitle[0].substring(
                        midTitle[0].lastIndexOf("/") + 1,
                        midTitle[0].length() - 1).replace(".", "/");
                String title = NlpUtils.unquoteName(midTitle[1].substring
                        ("/wikipedia/en/".length() + 1,
                                midTitle[1].length() - 1));
                out.println(title + "\t" + mid);

            }
            out.close();
            input.close();
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
