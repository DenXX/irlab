package edu.emory.mathcs.clir.relextract.tools;

import edu.umd.cloud9.collection.wikipedia.WikipediaPage;
import edu.umd.cloud9.collection.wikipedia.WikipediaPageInputFormat;
import edu.umd.cloud9.io.pair.PairOfStringLong;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Hadoop Map-Reduce job to extract all Wikipedia links phrases, count
 * how many times each phrase is used to refer to each page and return a link
 * phrase with the most frequent link page.
 */
public class WikipediaLinkPhrasesExtractor {

    /**
     * Mapper class to extract link phrase along with the page it links to.
     */
    public static class WikipediaLinkPhrasesExtractorMapper
            extends Mapper<LongWritable, WikipediaPage,
                           Text, PairOfStringLong> {

        /**
         * Iterates over all links on a Wikipedia page and outputs anchor text
         * and target page id.
         * @param key Mapper key, not used.
         * @param page Wikipedia Page to extract links from.
         * @param context MapReduce context.
         * @throws IOException
         * @throws InterruptedException
         */
        @Override
        public void map(LongWritable key, WikipediaPage page, Context context)
                throws IOException, InterruptedException {
            Text anchor = new Text();
            PairOfStringLong targetCount = new PairOfStringLong();
            for (WikipediaPage.Link link : page.extractLinks()) {
                anchor.set(link.getAnchorText());
                targetCount.set(link.getTarget(), 1);
                context.write(anchor, targetCount);
            }
        }
    }


    /**
     * Hadoop MapReduce combiner class, which aggregates counts for the same
     * anchor phrase-target page pairs.
     */
    public static class WikipediaLinkPhrasesExtractorCombiner
            extends Reducer<Text, PairOfStringLong, Text, PairOfStringLong> {

        /**
         * Aggregates counts for all targets and returns a hashmap, where key
         * is the target page id and value is the aggregated counts.
         * @param targetCounts A collection of target-count pairs to aggregate.
         * @return A hashmap, which maps targets to their counts.
         */
        public static HashMap<String, Long> aggregateTargetCounts(
                Iterable<PairOfStringLong> targetCounts) {
            HashMap<String, Long> targetToCount = new HashMap<String, Long>();
            for (PairOfStringLong targetCount : targetCounts) {
                if (!targetToCount.containsKey(targetCount.getKey())) {
                    targetToCount.put(targetCount.getKey(), 0L);
                }
                targetToCount.put(targetCount.getKey(),
                        targetToCount.get(targetCount.getKey()) +
                                targetCount.getValue());
            }
            return targetToCount;
        }

        /**
         * Accumulates and outputs the counts for the same anchor phrase and
         * target page.
         * @param phrase Current anchor phrase
         * @param targetCounts A list of target pages with their counts.
         * @param context MapReduce context.
         * @throws IOException
         * @throws InterruptedException
         */
        @Override
        public void reduce(Text phrase, Iterable<PairOfStringLong> targetCounts,
                           Context context)
                throws IOException, InterruptedException {

            PairOfStringLong outTargetCount = new PairOfStringLong();
            for (Map.Entry<String, Long> targetCount :
                    aggregateTargetCounts(targetCounts).entrySet()) {
                outTargetCount.set(targetCount.getKey(),
                        targetCount.getValue());
                context.write(phrase, outTargetCount);
            }
        }
    }

    /**
     * Reducer class, that further aggregates counts for the same anchor phrase
     * and target page and then outputs the target with the largest count.
     */
    public static class WikipediaLinkPhrasesExtractorReducer
            extends Reducer<Text, PairOfStringLong, Text, Text> {

        /**
         * Reducer aggregates counts for the same anchor phrase and target page
         * and outputs the target page with the largest.
         * @param phrase Anchor phrase.
         * @param targetCounts A collection of target page - count pairs to
         *                     process.
         * @param context MapReduce context.
         * @throws IOException
         * @throws InterruptedException
         */
        @Override
        protected void reduce(Text phrase,
                              Iterable<PairOfStringLong> targetCounts,
                              Context context)
                throws IOException,InterruptedException {

            String bestTarget = "";
            long bestCount = 0L;
            for (Map.Entry<String, Long> targetCount :
                    WikipediaLinkPhrasesExtractorCombiner.aggregateTargetCounts(
                            targetCounts).entrySet()) {
                if (bestCount < targetCount.getValue()) {
                    bestTarget = targetCount.getKey();
                    bestCount = targetCount.getValue();
                }
            }
            context.write(new Text(phrase), new Text(bestTarget));
        }
    }

    public static void main(String[] args) throws Exception {
        Job job = new Job(new Configuration());

        job.setJobName("ExtractWikipediaLinkPhrases");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(Text.class);

        job.setMapperClass(WikipediaLinkPhrasesExtractorMapper.class);
        job.setCombinerClass(WikipediaLinkPhrasesExtractorCombiner.class);
        job.setReducerClass(WikipediaLinkPhrasesExtractorReducer.class);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        // Delete the output directory if it exists already.
        FileSystem.get(new Configuration()).delete(new Path(args[1]), true);

        job.setInputFormatClass(WikipediaPageInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        job.setJarByClass(WikipediaLinkPhrasesExtractor.class);
        job.waitForCompletion(true);
    }

}
