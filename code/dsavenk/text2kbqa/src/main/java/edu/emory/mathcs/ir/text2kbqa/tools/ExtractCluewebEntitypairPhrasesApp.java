package edu.emory.mathcs.ir.text2kbqa.tools;

import lemur.nopol.ResponseIterator;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Extracts phrases around the mentions of entity pairs in Clueweb.
 */
public class ExtractCluewebEntitypairPhrasesApp {
    private static final String CLUEWEB_PATH_TEMPLATE =
            //"/home/dsavenk/Projects/%s/%s/ClueWeb12_%s/%s/%s-%s.warc.gz";
            "/%s/%s/ClueWeb12_%s/%s/%s-%s.warc.gz";

    public static final int BEFORE_OFFSET = -100;
    public static final int AFTER_OFFSET = 100;

    class EntityPairPhrase {
        String phrase;
        int start1;
        int end1;
        String mention;
        int start2;
        int end2;

        EntityPairPhrase(String phrase, int start1, int end1, String mention, int start2, int end2) {
            this.phrase = phrase;
            this.start1 = start1;
            this.end1 = end1;
            this.start2 = start2;
            this.end2 = end2;
        }
    }

    public static void main(String[] args) throws IOException {
        String entityPairsFile=args[0];
        String outFile=args[1];

        long nullPhrases = 0;

        try (BufferedReader reader =
                     new BufferedReader(
                             new InputStreamReader(
                                     new GZIPInputStream(
                                             new FileInputStream(
                                                     entityPairsFile))));
             BufferedWriter out =
                     new BufferedWriter(
                             new OutputStreamWriter(
                                     new GZIPOutputStream(
                                             new FileOutputStream(outFile))))
        ) {
            String line;
            String currentWarcFile = "";
            FileInputStream warcStream = null;
            ResponseIterator warcIterator = null;

            String currentWarcDocumentId = null;

            List<EntityPairRecord> currentDocumentRecords = new ArrayList<>();

            long index = 0;
            long startTime = System.nanoTime();
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
                        warcStream = new FileInputStream(currentWarcFile);
                        warcIterator = new ResponseIterator(warcStream);
                    }

                    lemur.nopol.ResponseIterator.WarcEntry thisWarcRecord =
                            readDocumentFromWarc(warcIterator, currentWarcDocumentId);
                    if (thisWarcRecord != null) {
                        for (EntityPairRecord record : currentDocumentRecords) {
                            String phrase = extractPhrasesAroundEntityPairs(
                                    record, thisWarcRecord);

                            if (phrase != null) {
                                out.write(String.format(
                                        "%s\t%s\t%s\t%s\t%s\t%s\n",
                                        record.documentId,
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
                    System.err.println(1e-9 * (System.nanoTime() - startTime) / index / 1000 + " per 1000 lines");
                }
            }
        } catch (Exception ex) {
            System.err.println(ex.toString());
            ex.printStackTrace();
        }
    }

    private static String extractPhrasesAroundEntityPairs(
            EntityPairRecord record, ResponseIterator.WarcEntry thisWarcRecord) throws UnsupportedEncodingException {
        try {
            int start1 = record.firstEntityBegin - thisWarcRecord.httpHeader.length;
            int end1 = record.firstEntityEnd - thisWarcRecord.httpHeader.length;
            int start2 = record.secondEntityBegin - thisWarcRecord.httpHeader.length;
            int end2 = record.secondEntityEnd - thisWarcRecord.httpHeader.length;

            String documentContent = new String(thisWarcRecord.content, record.encoding);
            byte[] content = documentContent.getBytes("UTF-8");

            int phraseStart = Math.max(0, Math.min(start1, start2) + BEFORE_OFFSET);
            int phraseEnd = Math.min(content.length, Math.max(end1, end2) + AFTER_OFFSET);

            String mention1 = normalizeString(new String(
                    Arrays.copyOfRange(content, start1, end1),
                    record.encoding), false);
            String mention2 = normalizeString(new String(
                    Arrays.copyOfRange(content, start2, end2),
                    record.encoding), false);

            String wholePhrase = new String(
                    Arrays.copyOfRange(content, phraseStart, phraseEnd),
                    record.encoding);

            String phrase = normalizeString(wholePhrase, true);

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
            return phrase.replace(mention1, "<E1>" + mention1 + "</E1>").replace(mention2, "<E2>" + mention2 + "</E2>");
        } catch (UnsupportedEncodingException | IndexOutOfBoundsException e) {
            System.err.println(e.toString());
        }
        return null;
    }

    private static String normalizeString(String str, boolean stripWordCuts) throws UnsupportedEncodingException {
        String tmp = str.replaceAll("&[a-zA-Z#0-9]{1,6};", " ")
                .replaceAll("<.*?>","")
                .replaceAll("^.*?>","")
                .replaceAll("<.*?$","")
                .replaceAll("\\s+", " ").replace("\n", " ").replace("\t", " ");
        return stripWordCuts ? tmp.replaceAll("^[^\\s>]*?(\\s|>)","").replaceAll("(\\s|<)[^\\s<]*?$","") : tmp;

    }


    private static ResponseIterator.WarcEntry readDocumentFromWarc(
            ResponseIterator warcIterator, String currentWarcDocumentId)
            throws IOException {
        lemur.nopol.ResponseIterator.WarcEntry thisWarcRecord;
        do {
            thisWarcRecord = warcIterator.next();
        } while (warcIterator.hasNext() && thisWarcRecord != null &&
                !currentWarcDocumentId.equals(thisWarcRecord.trecId));
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
    String encoding;
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
            rec.encoding = fields[1];
            rec.firstEntityMid = fields[2];
            rec.firstEntityName = fields[3];
            rec.secondEntityMid = fields[4];
            rec.secondEntityName = fields[5];
            rec.firstEntityBegin = Integer.parseInt(fields[6]);
            rec.firstEntityEnd = Integer.parseInt(fields[7]);
            rec.secondEntityBegin = Integer.parseInt(fields[8]);
            rec.secondEntityEnd = Integer.parseInt(fields[9]);
        } catch (Exception e) {
            System.err.println(e.toString());
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
