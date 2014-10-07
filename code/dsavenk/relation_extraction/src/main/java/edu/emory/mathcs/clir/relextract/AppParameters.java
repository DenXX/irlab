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
        opt.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName(INPUT_PARAMETER)
                .withDescription("input file").create(INPUT_PARAMETER));
        opt.addOption(OptionBuilder.isRequired(true).hasArg().
                withArgName(OUTPUT_PARAMETER).withDescription("output file")
                .create(OUTPUT_PARAMETER));
        opt.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName("processors").withDescription("Processors to run")
                .create(PROCESSORS_PARAMETER));

        // TODO(denxx): Move this to the corresponding classes and use
        // reflection.
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(EntityResolutionAnnotator.LEXICON_PROPERTY)
                .withDescription("entity names lexicon file").create(
                        EntityResolutionAnnotator.LEXICON_PROPERTY));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(ProcessorRunner.NUM_THREADS_PROPERTY)
                .withDescription("Number of threads to use").create(
                        ProcessorRunner.NUM_THREADS_PROPERTY));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(KnowledgeBase.KB_PROPERTY)
                .withDescription("Apache Jena KB model location").create(
                        KnowledgeBase.KB_PROPERTY));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(BatchSerializerProcessor.BATCH_SIZE_PARAMETER)
                .withDescription("Batch size in documents").create(
                        BatchSerializerProcessor.BATCH_SIZE_PARAMETER));

        opt.addOption(OptionBuilder.hasArg()
                .withArgName(ProcessorRunner.NUM_THREADS_PROPERTY)
                .withDescription("Number of threads").create(
                        ProcessorRunner.NUM_THREADS_PROPERTY));
        return opt;
    }
}
