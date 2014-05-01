package edu.emory.cqaqa.tools;

import com.hp.hpl.jena.rdf.model.Model;
import com.hp.hpl.jena.rdf.model.ModelFactory;
import com.hp.hpl.jena.rdf.model.Statement;
import com.hp.hpl.jena.rdf.model.StmtIterator;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Created by dsavenk on 4/30/14.
 */
public class Freebase {
    private static Model freebaseModel = ModelFactory.createDefaultModel();

    public static void readFreebase(String filename) throws IOException {
        freebaseModel.read(new GZIPInputStream(new FileInputStream(filename)), null, "TURTLE");
        StmtIterator iterator = freebaseModel.listStatements();
        while (iterator.hasNext()) {
            Statement statement = iterator.nextStatement();
            System.out.println(statement.getObject() + " - " + statement.getPredicate() + " - " + statement.getSubject());
        }
    }
}
