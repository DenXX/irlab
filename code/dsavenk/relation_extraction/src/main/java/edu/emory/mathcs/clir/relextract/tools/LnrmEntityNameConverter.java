package edu.emory.mathcs.clir.relextract.tools;

import edu.emory.mathcs.clir.relextract.utils.NlpUtils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * Created by dsavenk on 11/11/14.
 */
public class LnrmEntityNameConverter {
    public static void main(String[] args) throws IOException {
        BufferedReader input = new BufferedReader(new FileReader(args[0]));

        String line;
        while ((line = input.readLine()) != null) {
            System.out.println(NlpUtils.normalizeStringForMatch(line));
        }
        input.close();
    }
}
