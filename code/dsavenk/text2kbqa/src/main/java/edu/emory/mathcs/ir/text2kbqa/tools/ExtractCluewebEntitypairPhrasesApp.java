package edu.emory.mathcs.ir.text2kbqa.tools;

import edu.cmu.lemurproject.WarcHTMLResponseRecord;
import edu.cmu.lemurproject.WarcRecord;

import java.io.*;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Extracts phrases around the mentions of entity pairs in Clueweb.
 */
public class ExtractCluewebEntitypairPhrasesApp {
    private static final String CLUEWEB_PATH_TEMPLATE =
            "/home/dsavenk/Projects/%s/%s/ClueWeb12_%s/%s/%s-%s.warc.gz";

    public static void main(String[] args) throws IOException {
        String entityPairsFile=args[0];

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(
                    new GZIPInputStream(
                        new FileInputStream(entityPairsFile))))) {
            String line;
            String currentWarcFile = "";
            DataInputStream warcStream = null;

            String currentWarcDocumentId = null;

            List<EntityPairRecord> currentDocumentRecords = new ArrayList<>();

            while ((line = reader.readLine()) != null) {
                EntityPairRecord rec = EntityPairRecord.parseRecord(line);
                String newWarcFile = getWarcFilePath(rec.documentId);

                // If the name of the file of the current document changed from
                // the previous one, we need to open another Clueweb web
                // archive.
                if (!newWarcFile.equals(currentWarcFile)) {
                    if (warcStream != null) {
                        warcStream.close();
                    }
                    currentWarcFile = newWarcFile;
                    warcStream = new DataInputStream(
                            new GZIPInputStream(
                                    new FileInputStream(currentWarcFile)));
                }

                // We keep aggregating records for the same document, after that
                // we will read this document and extract phrases of interest.
                if (currentWarcDocumentId != null &&
                        !currentWarcDocumentId.equals(rec.documentId)) {
                    WarcRecord thisWarcRecord = readDocumentFromWarc(
                            warcStream, currentWarcDocumentId);
                    if (thisWarcRecord != null) {
                        for (EntityPairRecord record : currentDocumentRecords) {
                            String phrase = extractPhrasesAroundEntityPairs(
                                    record, thisWarcRecord);
                            if (phrase != null) {
                                System.out.println(phrase);
                            }
                        }
                    }
                    currentDocumentRecords.clear();
                }

                currentWarcDocumentId = rec.documentId;
                // Add record to the list.
                currentDocumentRecords.add(rec);
            }
        }
    }

    private static String extractPhrasesAroundEntityPairs(
            EntityPairRecord record,
            WarcRecord thisWarcRecord) {
        int start = Math.max(0,
                Math.min(record.firstEntityBegin,
                        record.secondEntityBegin) - 200);
        int end = Math.min(thisWarcRecord.getContent().length,
                Math.max(record.firstEntityEnd,
                        record.secondEntityEnd) + 200);
        try {
            return new String(
                    thisWarcRecord.getContent(), start, end - start, "UTF8")
                    .replace("\n", " ");
        } catch (UnsupportedEncodingException e) {
            System.err.println(e.getMessage());
        }
        return null;
    }

    private static WarcRecord readDocumentFromWarc(
            DataInputStream warcStream, String currentWarcDocumentId)
            throws IOException {
        WarcRecord thisWarcRecord;
        do {
            thisWarcRecord =
                    WarcRecord.readNextWarcRecord(warcStream);
        } while (thisWarcRecord != null &&
                (!thisWarcRecord.getHeaderRecordType().equals("response") ||
                        !currentWarcDocumentId.equals(
                                thisWarcRecord.getHeaderMetadataItem(
                                        "WARC-TREC-ID"))));
        return thisWarcRecord;
    }

    private static String getWarcFilePath(String fileId) {
        String[] parts = fileId.split("-");
        String cluewebPathStr = parts[1].substring(0, 2);
        int cluewebPartNo = Integer.parseInt(cluewebPathStr);
        String cluewebDir = cluewebPartNo < 10 ?
                "ClueWeb1" : "ClueWeb2";
        String disk;
        if (cluewebPartNo < 5) disk = "Disk1";
        else if (cluewebPartNo < 10) disk = "Disk2";
        else if (cluewebPartNo < 15) disk = "Disk3";
        else disk = "Disk4";

        return String.format(CLUEWEB_PATH_TEMPLATE,
                cluewebDir, disk, cluewebPathStr, parts[1], parts[1], parts[2]);
    }
}


class EntityPairRecord {
    String documentId;
    String firstEntityMid;
    String secondEntityMid;
    String firstEntityName;
    String secondEntityName;
    int firstEntityBegin;
    int firstEntityEnd;
    int secondEntityBegin;
    int secondEntityEnd;

    private EntityPairRecord() {}

    static EntityPairRecord parseRecord(String line) {
        EntityPairRecord rec = new EntityPairRecord();
        String[] fields = line.split("\t");
        rec.documentId = fields[0];
        rec.firstEntityMid = fields[1];
        rec.firstEntityName = fields[2];
        rec.secondEntityMid = fields[3];
        rec.secondEntityName = fields[4];
        rec.firstEntityBegin = Integer.parseInt(fields[5]);
        rec.firstEntityEnd = Integer.parseInt(fields[6]);
        rec.secondEntityBegin = Integer.parseInt(fields[7]);
        rec.secondEntityEnd = Integer.parseInt(fields[8]);
        return rec;
    }
}