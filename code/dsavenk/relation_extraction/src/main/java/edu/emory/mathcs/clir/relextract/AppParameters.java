package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.annotators.EntityResolutionAnnotator;
import edu.emory.mathcs.clir.relextract.processor.BatchSerializerProcessor;
import edu.emory.mathcs.clir.relextract.utils.KnowledgeBase;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * Created by dsavenk on 10/7/14.
 */
public class AppParameters {
    public static final String INPUT_PARAMETER = "input";
    public static final String OUTPUT_PARAMETER = "output";
    public static final String PROCESSORS_PARAMETER = "processors";
    public static final String RUNNER_PARAMETER = "runner";

    public static final Options options = initOptions();

    private static Options initOptions() {
        Options opt = new Options();
        options.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName("input_path")
                .withDescription("input file").create(INPUT_PARAMETER));
        options.addOption(OptionBuilder.isRequired(true).hasArg().
                withArgName("output_path").withDescription("output file")
                .create(OUTPUT_PARAMETER));
        options.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName("processors").withDescription("Processors to run")
                .create(PROCESSORS_PARAMETER));
        options.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName(RUNNER_PARAMETER).withDescription("Runner to use")
                .create(RUNNER_PARAMETER));

        // TODO(denxx): Move this to the corresponding classes and use
        // reflection.
        options.addOption(OptionBuilder.hasArg()
                .withArgName(EntityResolutionAnnotator.LEXICON_PROPERTY)
                .withDescription("entity names lexicon file").create(
                        EntityResolutionAnnotator.LEXICON_PROPERTY));
        options.addOption(OptionBuilder.hasArg()
                .withArgName(ConcurrentProcessingRunner.NUM_THREADS_PROPERTY)
                .withDescription("Number of threads to use").create(
                        ConcurrentProcessingRunner.NUM_THREADS_PROPERTY));
        options.addOption(OptionBuilder.hasArg()
                .withArgName(KnowledgeBase.KB_PROPERTY)
                .withDescription("Apache Jena KB model location").create(
                        KnowledgeBase.KB_PROPERTY));
        options.addOption(OptionBuilder.hasArg()
                .withArgName(BatchSerializerProcessor.BATCH_SIZE_PARAMETER)
                .withDescription("Batch size in documents").create(
                        BatchSerializerProcessor.BATCH_SIZE_PARAMETER));
        return opt;
    }
}
