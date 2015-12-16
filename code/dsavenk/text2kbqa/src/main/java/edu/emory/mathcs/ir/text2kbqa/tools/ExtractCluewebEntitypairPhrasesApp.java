package edu.emory.mathcs.ir.text2kbqa.tools;

import edu.cmu.lemurproject.WarcRecord;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.GZIPInputStream;

/**
 * Extracts phrases around the mentions of entity pairs in Clueweb.
 */
public class ExtractCluewebEntitypairPhrasesApp {
    private static final String CLUEWEB_PATH_TEMPLATE =
            //"/home/dsavenk/Projects/%s/%s/ClueWeb12_%s/%s/%s-%s.warc.gz";
            "/%s/%s/ClueWeb12_%s/%s/%s-%s.warc.gz";

    public static void main(String[] args) throws IOException {
        String entityPairsFile=args[0];

        long nullPhrases = 0;

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new GZIPInputStream(
                                             new FileInputStream(
                                                     entityPairsFile))));
             BufferedWriter out =
                     new BufferedWriter(
                             new OutputStreamWriter(System.out))
        ) {
            String line;
            String currentWarcFile = "";
            DataInputStream warcStream = null;

            String currentWarcDocumentId = null;

            List<EntityPairRecord> currentDocumentRecords = new ArrayList<>();

            long index = 0;
            while ((line = reader.readLine()) != null) {
                EntityPairRecord currentEntityPairRecord =
                        EntityPairRecord.parseRecord(line);
                if (currentEntityPairRecord == null) continue;

                // We keep aggregating records for the same document, after that
                // we will read this document and extract phrases of interest.
                if (currentWarcDocumentId != null &&
                        !currentWarcDocumentId.equals(
                                currentEntityPairRecord.documentId)) {
                    // If the name of the file of the current document changed from
                    // the previous one, we need to open another Clueweb web
                    // archive.
                    String warcFile = getWarcFilePath(currentWarcDocumentId);
                    if (!warcFile.equals(currentWarcFile)) {
                        if (warcStream != null) {
                            warcStream.close();
                        }
                        currentWarcFile = warcFile;
                        warcStream = new DataInputStream(
                                new GZIPInputStream(
                                        new FileInputStream(currentWarcFile)));
                    }

                    WarcRecord thisWarcRecord = readDocumentFromWarc(
                            warcStream, currentWarcDocumentId);
                    if (thisWarcRecord != null) {
                        for (EntityPairRecord record : currentDocumentRecords) {
                            String phrase = extractPhrasesAroundEntityPairs(
                                    record, thisWarcRecord);

                            if (phrase != null) {
                                out.write(String.format(
                                        "%s\t%s\t%s\t%s\t%s\n",
                                        record.firstEntityMid,
                                        record.firstEntityName,
                                        record.secondEntityMid,
                                        record.secondEntityName,
                                        phrase));
                            } else {
                                ++nullPhrases;
                            }
                        }
                    }
                    currentDocumentRecords.clear();
                }

                currentWarcDocumentId = currentEntityPairRecord.documentId;
                // Add record to the list.
                currentDocumentRecords.add(currentEntityPairRecord);

                if (++index % 100000 == 0) {
                    System.err.println(String.format(
                            "%d lines processed, %d lines skipped",
                            index, nullPhrases));
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.getMessage());
        }
    }

    private static String extractPhrasesAroundEntityPairs(
            EntityPairRecord record,
            WarcRecord thisWarcRecord) {
        int start = Math.max(0,
                Math.min(record.firstEntityBegin,
                        record.secondEntityBegin) - 100);
        int end = Math.min(thisWarcRecord.getContent().length,
                Math.max(record.firstEntityEnd,
                        record.secondEntityEnd) + 100);
        try {
            String wholePhrase =
                new String(thisWarcRecord.getContent(), start, end - start, "UTF8");
            String phrase = wholePhrase
                    .replaceAll("&[a-zA-Z#0-9]{1,5};", " ")
                    .replaceAll("^[^\\s>]*?(\\s|>)","")
                    .replaceAll("(\\s|<)[^\\s<]*?$","")
                    .replaceAll("<.*?>","")
                    .replaceAll("^.*?>","")
                    .replaceAll("<.*?$","")
                    .replaceAll("\\s+", " ");

            // Check that the phrase actually contains the names.
            String phraseToCheck =
                    phrase.replaceAll("[^a-zA-Z]*", "").toLowerCase();
            String firstEntityName = record.firstEntityName
                    .replaceAll("[^a-zA-Z]*", "").toLowerCase();
            String secondEntityName = record.secondEntityName
                    .replaceAll("[^a-zA-Z]*", "").toLowerCase();
            if (!phraseToCheck.contains(firstEntityName) ||
                    !phraseToCheck.contains(secondEntityName)) {
                return null;
            }
            return phrase;
        } catch (UnsupportedEncodingException | IndexOutOfBoundsException e) {
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
        try {
            rec.documentId = fields[0];
            rec.firstEntityMid = fields[1];
            rec.firstEntityName = fields[2];
            rec.secondEntityMid = fields[3];
            rec.secondEntityName = fields[4];
            rec.firstEntityBegin = Integer.parseInt(fields[5]);
            rec.firstEntityEnd = Integer.parseInt(fields[6]);
            rec.secondEntityBegin = Integer.parseInt(fields[7]);
            rec.secondEntityEnd = Integer.parseInt(fields[8]);
        } catch (Exception e) {
            System.err.println(e.getMessage());
            System.err.println(line);
	        return null;
	    }
        return rec;
    }

    @Override
    public String toString() {
        return String.format("%s\t%s\t%s\t%s\t%s\t%d\t%d\t%d\t%d\t",
                this.documentId, this.firstEntityMid, this.firstEntityName,
                this.secondEntityMid, this.secondEntityName, firstEntityBegin,
                firstEntityEnd, secondEntityBegin, secondEntityEnd);
    }
}
