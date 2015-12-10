package edu.emory.mathcs.clir.relextract;

import edu.emory.mathcs.clir.relextract.data.*;
import edu.emory.mathcs.clir.relextract.processor.*;
import org.apache.commons.cli.*;

import javax.xml.stream.XMLStreamException;
import java.io.FileNotFoundException;
import java.util.Properties;

/**
 * Created by dsavenk on 9/12/14.
 */
public class RelationExtractionApp {

    // TODO(denxx): There should be a much better way to add options.
    private static Options getOptions() {
        return AppParameters.options;
    }

    private static CommandLine parseCommandLine(
            Options options, String[] args) throws ParseException {
        CommandLineParser parser = new GnuParser();
        return parser.parse(options, args);
    }

    private static Properties getProperties(CommandLine cmdline) {
        Properties props = new Properties();
        for (Option option : cmdline.getOptions()) {
            props.put(option.getArgName(), option.hasArg() ? option.getValue() : "");
        }

        return props;
    }

    private static void run(Properties props) throws Exception {
        WorkflowProcessor workflow = new WorkflowProcessor(props);
        for (String processor : props.getProperty(
                AppParameters.PROCESSORS_PARAMETER).split(",")) {
            switch (processor) {
                case "setdocid":
                    workflow.addProcessor(new SetDocIdProcessor(props));
                    break;
                case "corenlp":
                    workflow.addProcessor(new StanfordCoreNlpProcessor(props));
                    break;
                case "adddepth":
                    workflow.addProcessor(new AddDependencyTreeDepthProcessor(props));
                    break;
                case "entityres":
                    workflow.addProcessor(new EntityResolutionProcessor(props));
                    break;
                case "luceneentityres":
                    workflow.addProcessor(new LuceneEntityResolutionProcessor(props));
                    break;
                case "cascadeentityres":
                    workflow.addProcessor(new CascadeEntityResolutionProcessor(props));
                    break;
                case "addrelations":
                    workflow.addProcessor(new EntityRelationsLookupProcessor(props));
                    break;
                case "relstats":
                    workflow.addProcessor(new RelationsStatsProcessor(props));
                    break;
                case "rellocstats":
                    workflow.addProcessor(new RelationLocationStatsProcessor(props));
                    break;
                case "sentencetraining":
                    workflow.addProcessor(new SentenceBasedRelationExtractorTrainEvalProcessor(props));
                    break;
                case "qatraining":
                    workflow.addProcessor(new QuestionAnswerBasedRelationExtractorTrainEvalProcessor(props));
                    break;
                case "filter":
                    workflow.addProcessor(new FilterNotresolvedEntitiesProcessor(props));
                    break;
                case "serialize":
                    workflow.addProcessor(new SerializerProcessor(props));
                    break;
                case "textserialize":
                    workflow.addProcessor(new TextSerializerProcessor(props));
                    break;
                case "batchserialize":
                    workflow.addProcessor(new BatchSerializerProcessor(props));
                    break;
                case "dumpentitynames":
                    workflow.addProcessor(new DumpEntityNamesProcessor(props));
                    break;
                case "print":
                    workflow.addProcessor(new PrintTextProcessor(props));
                    break;
                case "test":
                    workflow.addProcessor(new TestProcessor(props));
                    break;
                case "dumptriples":
                    workflow.addProcessor(new TriplesDumpProcessor(props));
                    break;
                case "addsoftdaterelations":
                    workflow.addProcessor(new EntityAddSoftDateRelationsProcessor(props));
                    break;
                case "filterunresolvedspans":
                    workflow.addProcessor(new FilterNonResolvedSpans(props));
                    break;
                case "filterq":
                    workflow.addProcessor(new FilterQuestionsProcessor(props));
                    break;
                case "outputtemplate":
                    workflow.addProcessor(new OutputQuestionTemplateProcessor(props));
                    break;
                case "filterlang":
                    workflow.addProcessor(new FilterByLanguageProcessor(props));
                    break;
                case "cluster":
                    workflow.addProcessor(new ClusterQuestionsProcessor(props));
                    break;
                case "answerdocid":
                    workflow.addProcessor(new SetAnswerTextAsDocIdProcessor(props));
                    break;
                case "printentitytypequestions":
                    workflow.addProcessor(new PrintQuestionsForEntityTypes(props));
                    break;
                case "keeprepresentativeids":
                    workflow.addProcessor(new KeepRepresentativeMentionEntityIdProcessor(props));
                    break;
                case "qainstancebuilder":
                    workflow.addProcessor(new QAExamplesBuilderProcessor(props));
                    break;
                case "qa":
                    workflow.addProcessor(new QAModelTrainerProcessor(props));
                    break;
                case "index":
                    workflow.addProcessor(new BuildSearchIndexProcessor(props));
                    break;
                case "preddict":
                    workflow.addProcessor(new CollectPredicateDictProcessor(props));
                    break;
                case "windowspans":
                    workflow.addProcessor(new SlidingWindowSpanCreatorProcessor(props));
                    break;
                case "extractrecommend":
                    workflow.addProcessor(new RecommendationExtractionProcessor(props));
                    break;

                default:
                    throw new UnsupportedOperationException(
                            "Processor " + processor + " doesn't exist!");
            }
        }
        workflow.freeze();

        try {
            Iterable<Document.NlpDocument> docs = null;
            final String reader = props.getProperty(
                    AppParameters.READER_PARAMETER);
            if (props.getProperty(AppParameters.INPUT_PARAMETER).contains(MultipleFilesInputProvider.NAMES_SEPARATOR)) {
                Class<? extends Iterable<Document.NlpDocument>> baseProvider = null;
                switch (reader) {
                    case "text":
                        baseProvider = TextInputProvider.class;
                        break;
                    case "batchser":
                        baseProvider = DeserializerBatchInputProvider.class;
                        break;
                    case "ser":
                        baseProvider = DeserializerInputProvider.class;
                        break;
                    case "yahooxml":
                        baseProvider = YahooAnswersWebscopeXmlInputProvider.class;
                        break;
                    case "wikianswers":
                        baseProvider = WikiAnswersInputProvider.class;
                        break;
                    case "wikianswersall":
                        baseProvider = WikiAnswersAllQuestionsInputProvider.class;
                        break;
                    case "webquestions":
                        baseProvider = WebQuestionsInputProvider.class;
                        break;
                }
                docs = new MultipleFilesInputProvider<>(props, baseProvider);
            } else {
                switch (reader) {
                    case "text":
                        docs = new TextInputProvider(props);
                        break;
                    case "batchser":
                        docs = new DeserializerBatchInputProvider(props);
                        break;
                    case "ser":
                        docs = new DeserializerInputProvider(props);
                        break;
                    case "yahooxml":
                        docs = new YahooAnswersWebscopeXmlInputProvider(props);
                        break;
                    case "wikianswers":
                        docs = new WikiAnswersInputProvider(props);
                        break;
                    case "wikianswersall":
                        docs = new WikiAnswersAllQuestionsInputProvider(props);
                        break;
                    case "webquestions":
                        docs = new WebQuestionsInputProvider(props);
                        break;

                    default:
                        throw new UnsupportedOperationException("Reader " + reader +
                                " doesn't exist!");
                }
            }
            new ProcessorRunner(workflow, props).run(docs);
        } catch (FileNotFoundException e) {
            System.err.println("Cannot find input file.");
        } catch (XMLStreamException e) {
            System.err.println("Error parsing input XML file.");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws Exception {
        Options options = getOptions();
        CommandLine cmdline = null;

        try {
            cmdline = parseCommandLine(options, args);
        } catch (ParseException exc) {
            System.err.println("Error parsing command line: " +
                    exc.getMessage());
            System.exit(-1);
        }

        RelationExtractionApp.run(getProperties(cmdline));
    }
}
