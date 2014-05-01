package edu.emory.cqaqa.processor;

import edu.emory.cqaqa.types.QuestionAnswerPair;

import java.util.ArrayList;
import java.util.List;

/**
 * A pipeline of CqaPostProcessors.
 */
public class ProcessorPipeline implements QuestionAnswerPairProcessor {
    // Processor to apply in a row.
    private List<QuestionAnswerPairProcessor> processors = new ArrayList<QuestionAnswerPairProcessor>();

    /**
     * Adds a processor to a pipeline.
     * @param processor A processor to add.
     */
    public void addProcessor(QuestionAnswerPairProcessor processor) {
        processors.add(processor);
    }

    /**
     * Pass a post to a sequence of processors. Stops if one of the processors returns null.
     * @param post A post to process.
     * @return Result of the last processor or null if one of the processors return null.
     */
    @Override
    public QuestionAnswerPair processPair(QuestionAnswerPair post) {
        QuestionAnswerPair res = null;
        for (QuestionAnswerPairProcessor processor : processors) {
            if ((res = processor.processPair(post)) == null) {
                return null;
            }
        }
        return res;
    }
}
