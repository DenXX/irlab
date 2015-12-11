package edu.emory.mathcs.ir.text2kbqa.tools;

import edu.cmu.lemurproject.WarcHTMLResponseRecord;
import edu.cmu.lemurproject.WarcRecord;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;

/**
 * Extracts phrases around the mentions of entity pairs in Clueweb.
 */
public class ExtractCluewebEntitypairPhrasesApp {
    public static void main(String[] args) throws IOException {
        String inputWarcFile=args[0];

        // open our gzip input stream
        try (DataInputStream inStream =
                     new DataInputStream(
                             new GZIPInputStream(
                                     new FileInputStream(inputWarcFile)))) {
            WarcRecord thisWarcRecord;
            while ((thisWarcRecord = WarcRecord.readNextWarcRecord(inStream))
                    != null) {
                if (thisWarcRecord.getHeaderRecordType().equals("response")) {
                    // it is - create a WarcHTML record
                    WarcHTMLResponseRecord htmlRecord =
                            new WarcHTMLResponseRecord(thisWarcRecord);
                    // get our TREC ID and target URI
                    String thisTRECID = htmlRecord.getTargetTrecID();
                    String thisTargetURI = htmlRecord.getTargetURI();
                    // print our data
                    System.out.println(thisTRECID + " : " + thisTargetURI);
                }
            }
        }
    }
}
