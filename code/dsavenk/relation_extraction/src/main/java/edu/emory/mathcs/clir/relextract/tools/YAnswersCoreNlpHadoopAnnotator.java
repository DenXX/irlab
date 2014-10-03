package edu.emory.mathcs.clir.relextract.tools;

import com.twitter.elephantbird.mapreduce.io.ProtobufWritable;
import edu.emory.mathcs.clir.relextract.annotators.EntityResolutionAnnotator;
import edu.emory.mathcs.clir.relextract.data.QuestionAnswerAnnotation;
import edu.emory.mathcs.clir.relextract.utils.CoreNLPProtos;
import edu.emory.mathcs.clir.relextract.utils.ProtobufAnnotationSerializer;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import edu.umd.cloud9.collection.XMLInputFormat;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.conf.Configured;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.LongWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.Mapper;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.output.FileOutputFormat;
import org.apache.hadoop.mapreduce.lib.output.TextOutputFormat;
import org.apache.hadoop.util.Tool;
import org.apache.hadoop.util.ToolRunner;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;

/**
 * Hadoop Map-Reduce job to extract all Wikipedia links phrases, count
 * how many times each phrase is used to refer to each page and return a link
 * phrase with the most frequent link page.
 */
public class YAnswersCoreNlpHadoopAnnotator extends Configured implements Tool {
    public static void main(String[] args) throws Exception {
        Configuration conf = new Configuration();
        int res = ToolRunner.run(conf,
                new YAnswersCoreNlpHadoopAnnotator(), args);
        System.exit(res);
    }

    @Override
    public final int run(final String[] args) throws Exception {
        Configuration conf = getConf();
        conf.set(XMLInputFormat.START_TAG_KEY, "<vespaadd>");
        conf.set(XMLInputFormat.END_TAG_KEY, "</vespaadd>");
        conf.set(EntityResolutionAnnotator.LEXICON_PROPERTY,
                "/home/dsavenk/ir/data/Freebase/freebase-lexicon.gz");
        conf.set("mapreduce.map.memory.mb", "4096");
        //conf.set("mapreduce.reduce.memory.mb", "20480");
        conf.set("mapreduce.map.java.opts", "-Xmx3800M");
        //conf.set("mapreduce.reduce.java.opts", "-Xmx16384M");
        Job job = new Job(conf);

        job.setJobName("CoreNLP_YAnswers_Annotation");

        job.setOutputKeyClass(Text.class);
        job.setOutputValueClass(ProtobufWritable.class);

        job.setMapperClass(YAnswersCoreNlpHadoopAnnotatorMapper.class);
        job.setReducerClass(Reducer.class);

        FileInputFormat.setInputPaths(job, new Path(args[0]));
        FileOutputFormat.setOutputPath(job, new Path(args[1]));
        // Delete the output directory if it exists already.
        FileSystem.get(conf).delete(new Path(args[1]), true);

        job.setInputFormatClass(XMLInputFormat.class);
        job.setOutputFormatClass(TextOutputFormat.class);

        job.setJarByClass(YAnswersCoreNlpHadoopAnnotator.class);
        job.waitForCompletion(true);
        return 0;
    }

    /**
     * Mapper class to extract link phrase along with the page it links to.
     */
    public static class YAnswersCoreNlpHadoopAnnotatorMapper
            extends Mapper<LongWritable, Text, Text,
            com.twitter.elephantbird.mapreduce.io.ProtobufWritable<
                    CoreNLPProtos.Document>> {

        ProtobufAnnotationSerializer protoSerializer_ =
                new ProtobufAnnotationSerializer();
        ProtobufWritable<CoreNLPProtos.Document> protoWritable_ =
                ProtobufWritable.newInstance(CoreNLPProtos.Document.class);
        private StanfordCoreNLP nlpPipeline_;
        private DocumentBuilder xmlDocBuilder_;

        @Override
        public void setup(Context context) {
            Properties properties = new Properties();
            // Adds custom CoreNLP annotators.
//            properties.setProperty("customAnnotatorClass.entityres",
//                    "edu.emory.mathcs.clir.relextract.annotators." +
//                            "EntityResolutionAnnotator");
//            properties.setProperty("customAnnotatorClass.span",
//                    "edu.emory.mathcs.clir.relextract.annotators.SpanAnnotator");


            // Sets the NLP pipeline and some of the annotator properties.
            properties.put("annotators", "tokenize, cleanxml, ssplit, pos, " +
                    "lemma, ner, parse, dcoref");

            // Set annotator properties.
//            properties.setProperty(EntityResolutionAnnotator.LEXICON_PROPERTY,
//                    context.getConfiguration().get(
//                            EntityResolutionAnnotator.LEXICON_PROPERTY));
            properties.setProperty("clean.allowflawedxml", "true");

            nlpPipeline_ = new StanfordCoreNLP(properties);
            DocumentBuilderFactory factory =
                    DocumentBuilderFactory.newInstance();
            try {
                xmlDocBuilder_ = factory.newDocumentBuilder();
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                xmlDocBuilder_ = null;
            }
        }

        @Override
        public void map(LongWritable key, Text page, Context context)
                throws IOException, InterruptedException {
//
            // create a new document from input source
            InputSource is = new InputSource(new StringReader(page.toString()));
            Document xmlDoc = null;
            try {
                xmlDoc = xmlDocBuilder_.parse(is);
            } catch (SAXException e) {
                e.printStackTrace();
                return;
            }

            assert xmlDoc != null;
            // get the <vespaadd> element.
            Queue<Node> nodeQueue = new LinkedList<>();
            nodeQueue.add(xmlDoc.getDocumentElement());
            String questionText = "";
            String answerText = "";
            Map<String, String> attrs = new HashMap<>();
            while (!nodeQueue.isEmpty()) {
                Node node = nodeQueue.poll();
                if (node.getNodeType() == Node.TEXT_NODE) {
                    String content = node.getNodeValue();
                    String parentNodeName = node.getParentNode().getNodeName();
                    switch (parentNodeName) {
                        case "subject":
                            questionText = content;
                            break;
                        case "bestanswer":
                            answerText = content;
                            break;
                        default:
                            if (!content.isEmpty()) {
                                attrs.put(parentNodeName, content);
                            }
                    }
                } else {
                    // Iterate over children and add them to the queue.
                    NodeList children = node.getChildNodes();
                    for (int i = 0; i < children.getLength(); ++i) {
                        nodeQueue.add(children.item(i));
                    }
                }
            }

            QuestionAnswerAnnotation qaDoc =
                    new QuestionAnswerAnnotation(questionText, answerText);

            for (Map.Entry<String, String> kv : attrs.entrySet()) {
                qaDoc.addAttribute(kv.getKey(), kv.getValue());
            }
            //            // Process the Q&A document and output the result.
            nlpPipeline_.annotate(qaDoc);
            CoreNLPProtos.Document protoDoc = protoSerializer_.toProto(qaDoc);
            protoWritable_.set(protoDoc);
            context.write(new Text(questionText), protoWritable_);
        }
    }

}
