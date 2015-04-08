package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.data.DocumentWrapper;

import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;

/**
 * Created by dsavenk on 4/8/15.
 */
public class CollectPredicateDictProcessor extends Processor {

    private final Map<String, AtomicLong> predCount_ = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, AtomicLong> lemmaCount_ = Collections.synchronizedMap(new HashMap<>());
    private final Map<String, Map<String, AtomicLong>> lemmaPredCount_ = Collections.synchronizedMap(new HashMap<>());

    private final String dictLocationPaths;

    public static final String QA_RELATION_WORD_DICT_OUT_PARAMETER = "qa_relword_dict_out";

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public CollectPredicateDictProcessor(Properties properties) {
        super(properties);
        dictLocationPaths = properties.getProperty(QA_RELATION_WORD_DICT_OUT_PARAMETER);
    }

    @Override
    protected Document.NlpDocument doProcess(Document.NlpDocument document) throws Exception {
        Set<String> positivePredicates = document.getQaInstanceList().stream()
                .filter(Document.QaRelationInstance::getIsPositive)
                .map(Document.QaRelationInstance::getPredicate)
                .map(x -> x.contains(".cvt.") ? x.substring(x.indexOf(".cvt.") + 5) : x)
                .collect(Collectors.toSet());
        DocumentWrapper documentWrapper = new DocumentWrapper(document);
        if (!positivePredicates.isEmpty()) {
            List<String> lemmas = documentWrapper.getQuestionLemmas();
            lemmas.stream()
                    .filter(x -> !x.isEmpty())
                    .forEach(x -> {
                        lemmaCount_.putIfAbsent(x, new AtomicLong(0));
                        lemmaCount_.get(x).incrementAndGet();
                    });
            for (String predicate : positivePredicates) {
                updatePredicateCounts(lemmas, predicate);

                for (String piece : predicate.split("\\.")) {
                    updatePredicateCounts(lemmas, piece);
                }
            }
            return document;
        }
        return null;
    }

    @Override
    public void finishProcessing() throws IOException {
        String[] outPaths = dictLocationPaths.split(",");

        writeIndividualProbs(predCount_, outPaths[0]);
        writeIndividualProbs(lemmaCount_, outPaths[1]);
        writeConditionalProbs(outPaths[2]);
    }

    private void writeConditionalProbs(String outPath) throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outPath))));
        for (String rel : predCount_.keySet()) {
            Map<String, AtomicLong> currentPredDict = this.lemmaPredCount_.get(rel);
            long total = currentPredDict.values().stream().mapToLong(AtomicLong::get).sum();
            int i = 0;
            for (String word : lemmaCount_.keySet()) {
                if (i > 0) out.write(" ");
                if (currentPredDict.containsKey(word)) {
                    out.write(String.valueOf(Math.log((currentPredDict.get(word).get() + 1.0) / (total + lemmaCount_.size()))));
                } else {
                    out.write("-inf");
                }
                ++i;
            }
            out.write("\n");
        }
        out.close();
    }

    private void writeIndividualProbs(Map<String, AtomicLong> counts, String path) throws IOException {
        BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(path))));
        int index = 0;
        long total = counts.values().stream().mapToLong(AtomicLong::get).sum();
        for (String rel : counts.keySet()) {
            out.write(rel);
            out.write(" ");
            out.write(String.valueOf(index));
            out.write(" ");
            out.write(String.valueOf(counts.get(rel)));
            out.write(" ");
            out.write(String.valueOf(Math.log(1.0 * counts.get(rel).get() / total)));
            out.write("\n");
            ++index;
        }
        out.close();
    }

    private void updatePredicateCounts(List<String> lemmas, String predicate) {
        predCount_.putIfAbsent(predicate, new AtomicLong(0));
        predCount_.get(predicate).incrementAndGet();
        lemmaPredCount_.putIfAbsent(predicate, Collections.synchronizedMap(new HashMap<>()));
        Map<String, AtomicLong> currentPredDict = lemmaPredCount_.get(predicate);
        lemmas.stream()
                .forEach(x -> {
                    currentPredDict.putIfAbsent(x, new AtomicLong(0));
                    currentPredDict.get(x).incrementAndGet();
                });
    }
}
