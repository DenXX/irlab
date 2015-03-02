package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.data.Document;
import edu.emory.mathcs.clir.relextract.processor.Processor;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Object of this class allows one to process a collection of documents with
 * some processor in parallel.
 */
public class ProcessorRunner {

    /**
     * The name of the property to specify the number of threads to use. If the
     * property is missing all available threads will be used.
     */
    public static final String NUM_THREADS_PROPERTY = "run_nthreads";
    private final Processor processor_;
    private final int numThreads_;

    public static int MAX_ATTEMPTS = 5;

    /**
     * Creates a new concurrent processing runner object, which encapsulates
     * a Processor and runs it in parallel over a collection of documents
     * provided through an iterator.
     *
     * @param processor  A processor to run over a collection of documents.
     * @param properties Some properties, e.g. it can specify the number of
     *                   threads to use.
     */
    public ProcessorRunner(Processor processor,
                           Properties properties) {
        processor_ = processor;
        if (!processor.isFrozen()) {
            processor.freeze();
        }
        numThreads_ = !properties.containsKey(NUM_THREADS_PROPERTY)
                ? Runtime.getRuntime().availableProcessors()
                : Integer.parseInt(
                properties.getProperty(NUM_THREADS_PROPERTY));
    }

    /**
     * Runs a processor over the provided collection of documents in parallel.
     * The number of threads to use can be specified in the Properties in the
     * constructor as the ConcurrentProcessingRunner.numThreads property.
     *
     * @param documents A collections of documents to process.
     * @throws InterruptedException
     */
    public void run(Iterable<Document.NlpDocument> documents)
            throws Exception {
        if (numThreads_ == 1) {
            final long startTime = System.currentTimeMillis();
            int counter = 0;
            int filtered = 0;
            for (Document.NlpDocument document : documents) {
                // Parser reads the next input on demand, that's why it cannot
                // predict that the input is over, so it will actually return null
                // element.
                if (document == null) continue;
                if (processor_.process(document) == null) {
                    ++filtered;
                }
                if (++counter % 1000 == 0) {
                    long currentTime = System.currentTimeMillis();
                    System.err.println("Processed: " + counter +
                            " (" + (1000.0 * counter /
                            (currentTime - startTime)) + " docs/sec)");
                    System.err.println("Filtered: " + filtered);
                }
            }
            processor_.finishProcessing();
        } else {
            BlockingQueue<Runnable> blockingQueue = new ArrayBlockingQueue<>(numThreads_ * 1000);

            ThreadPoolExecutor threadPool = new ThreadPoolExecutor(numThreads_,
                    numThreads_, 0L, TimeUnit.MILLISECONDS,
                    blockingQueue); //Executors.newFixedThreadPool(numThreads_);

            threadPool.setRejectedExecutionHandler((r, executor) -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                executor.execute(r);
            });

            final AtomicInteger counter = new AtomicInteger(0);
            final AtomicInteger filtered = new AtomicInteger(0);
            int skipped = 0;
            final long startTime = System.currentTimeMillis();
            for (final Document.NlpDocument document : documents) {
                // Parser reads the next input on demand, that's why it cannot
                // predict that the input is over, so it will actually return null
                // element.
                if (document == null) {
                    ++skipped;
                    System.err.println("Skipped: " + skipped);
                }
                threadPool.submit(() -> {
                    try {
                        if (processor_.process(document) == null) {
                            filtered.incrementAndGet();
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    int processed = counter.incrementAndGet();
                    if (processed % 1000 == 0) {
                        long currentTime = System.currentTimeMillis();
                        System.err.println("Processed: " + processed +
                                " (" + (1000.0 * processed /
                                (currentTime - startTime)) + " docs/sec)");
                        System.err.println("Filtered: " + filtered);
                    }
                });
            }
            threadPool.shutdown();
            threadPool.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
            processor_.finishProcessing();
        }
    }
}
