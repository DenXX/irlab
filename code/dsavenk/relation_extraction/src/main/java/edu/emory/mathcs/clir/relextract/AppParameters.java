package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.processor.*;
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
    public static final String READER_PARAMETER = "reader";
    public static final String LOGFILE_PARAMETER = "log";

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
                .withArgName(PROCESSORS_PARAMETER).withDescription("Processors to run")
                .create(PROCESSORS_PARAMETER));
        opt.addOption(OptionBuilder.isRequired(true).hasArg()
                .withArgName(READER_PARAMETER).withDescription("Reader to use")
                .create(READER_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(AppParameters.LOGFILE_PARAMETER)
                .withDescription("File to write logging information").create(
                        AppParameters.LOGFILE_PARAMETER));

        // TODO(denxx): Move this to the corresponding classes and use
        // reflection.
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(EntityResolutionProcessor.LEXICON_PARAMETER)
                .withDescription("entity names lexicon file").create(
                        EntityResolutionProcessor.LEXICON_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(ProcessorRunner.NUM_THREADS_PROPERTY)
                .withDescription("Number of threads to use").create(
                        ProcessorRunner.NUM_THREADS_PROPERTY));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(KnowledgeBase.KB_PROPERTY)
                .withDescription("Apache Jena KB model location").create(
                        KnowledgeBase.KB_PROPERTY));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(KnowledgeBase.CVT_PREDICATE_LIST_PARAMETER)
                .withDescription("File with CVT predicates").create(
                        KnowledgeBase.CVT_PREDICATE_LIST_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(BatchSerializerProcessor.BATCH_SIZE_PARAMETER)
                .withDescription("Batch size in documents").create(
                        BatchSerializerProcessor.BATCH_SIZE_PARAMETER));

        opt.addOption(OptionBuilder.hasArg()
                .withArgName(ProcessorRunner.NUM_THREADS_PROPERTY)
                .withDescription("Number of threads").create(
                        ProcessorRunner.NUM_THREADS_PROPERTY));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(DumpEntityNamesProcessor.ENTITY_NAMES_FILENAME)
                .withDescription("File to dump entity names").create(
                        DumpEntityNamesProcessor.ENTITY_NAMES_FILENAME));

        opt.addOption(OptionBuilder.hasArg()
                .withArgName(LuceneEntityResolutionProcessor.LUCENE_INDEX_PARAMETER)
                .withDescription("Entity names lucene index").create(
                        LuceneEntityResolutionProcessor.LUCENE_INDEX_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(LuceneEntityResolutionProcessor.LUCENE_SPELLCHECKINDEX_PARAMETER)
                .withDescription("Lucene spellcheck index").create(
                        LuceneEntityResolutionProcessor.LUCENE_SPELLCHECKINDEX_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.PREDICATES_LIST_PARAMETER)
                .withDescription("File with the list of predicates to train extractor for").create(
                        RelationExtractorTrainEvalProcessor.PREDICATES_LIST_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.DATASET_OUTFILE_PARAMETER)
                .withDescription("Name of the file to output training dataset to.").create(
                        RelationExtractorTrainEvalProcessor.DATASET_OUTFILE_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.MODEL_OUTFILE_PARAMETER)
                .withDescription("Name of the file to output trained model.").create(
                        RelationExtractorTrainEvalProcessor.MODEL_OUTFILE_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.SPLIT_TRAIN_TEST_TRIPLES_PARAMETER)
                .withDescription("True or False - whether to split triples into sets for training and test.").create(
                        RelationExtractorTrainEvalProcessor.SPLIT_TRAIN_TEST_TRIPLES_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.TYPES_OF_MENTIONS_TO_KEEP_PARAMETER)
                .withDescription("Types of mentions to keep for training and testing.").create(
                        RelationExtractorTrainEvalProcessor.TYPES_OF_MENTIONS_TO_KEEP_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(EntityRelationsLookupProcessor.CVT_PREDICATES_LIST_PARAMETER)
                .withDescription("File with the list of CVT predicates to use.").create(
                        EntityRelationsLookupProcessor.CVT_PREDICATES_LIST_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.MODEL_PARAMETER)
                .withDescription("File with serialized model to apply").create(
                        RelationExtractorTrainEvalProcessor.MODEL_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.QUESTION_FEATS_PARAMETER)
                .withDescription("Whether to include question features or not").create(
                        RelationExtractorTrainEvalProcessor.QUESTION_FEATS_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.NEGATIVE_SUBSAMPLE_PARAMETER)
                .withDescription("Which fraction of negative examples to remove from training").create(
                        RelationExtractorTrainEvalProcessor.NEGATIVE_SUBSAMPLE_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(RelationExtractorTrainEvalProcessor.REGALURIZATION_PARAMETER)
                .withDescription("L2 regularization parameter").create(
                        RelationExtractorTrainEvalProcessor.REGALURIZATION_PARAMETER));

        // Cascade entity resolver
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(CascaseEntityResolutionProcessor.WIKILINKS_DICTIONARY_PARAMETER)
                .withDescription("Wikilinks dictionary file").create(
                        CascaseEntityResolutionProcessor.WIKILINKS_DICTIONARY_PARAMETER));
        opt.addOption(OptionBuilder.hasArg()
                .withArgName(CascaseEntityResolutionProcessor.WIKILINKS_LNRM_DICTIONARY_PARAMETER)
                .withDescription("Wikilinks normalized dictionary file").create(
                        CascaseEntityResolutionProcessor.WIKILINKS_LNRM_DICTIONARY_PARAMETER));

        return opt;
    }
}
