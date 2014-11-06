package edu.emory.mathcs.clir.relextract.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by dsavenk on 11/6/14.
 */
public class FileUtils {

    /**
     * Reads lines from the given file and returns them as a list.
     *
     * @param fileName Name of the file to read.
     * @return List of strings containing lines read from a file.
     * @throws IOException raised if file doesn't exist or any other error
     *                     happens.
     */
    public static List<String> readLinesFromFile(String fileName)
            throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(fileName));
        List<String> res = new LinkedList<>();
        String line;
        while ((line = reader.readLine()) != null) {
            res.add(line);
        }
        return res;
    }
}
