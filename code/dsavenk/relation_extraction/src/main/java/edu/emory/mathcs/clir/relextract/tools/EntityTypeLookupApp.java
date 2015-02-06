package edu.emory.mathcs.clir.relextract.tools;

import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Created by dsavenk on 2/6/15.
 */
public class EntityTypeLookupApp {

    public static void main(String[] args) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(args[0]));
            Properties props = new Properties();
            props.setProperty("kb", args[1]);
            KnowledgeBase kb = KnowledgeBase.getInstance(props);
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line + "\t" + kb.getEntityTypes(line).stream().collect(Collectors.joining("\t")));
            }
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
    }
}
