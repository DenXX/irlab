package edu.emory.mathcs.clir.relextract.processor;

import edu.emory.mathcs.clir.relextract.data.Document;
import org.maltparser.concurrent.ConcurrentMaltParserModel;
import org.maltparser.concurrent.ConcurrentMaltParserService;
import org.maltparser.core.exception.MaltChainedException;

import java.util.Properties;

/**
 * Created by dsavenk on 10/2/14.
 */
public class MaltParserProcessor extends Processor {
    private final ConcurrentMaltParserModel parser_;

    /**
     * Processors can take parameters, that are stored inside the properties
     * argument.
     *
     * @param properties A set of properties, the processor doesn't have to
     *                   consume all of the them, it checks what it needs.
     */
    public MaltParserProcessor(Properties properties)
            throws MaltChainedException {
        super(properties);
        java.net.URL model =
                ClassLoader.getSystemResource("engmalt.linear-1.7.mco");
        parser_ = ConcurrentMaltParserService.initializeParserModel(model);
    }

    @Override
    protected Document.NlpDocument doProcess(
            Document.NlpDocument document) throws Exception {
//        int sentIndex = 0;
//        for (CoreMap sent : document.get(CoreAnnotations.SentencesAnnotation.class)) {
//            ++sentIndex;
//            List<String> tokensStr = new LinkedList<>();
//            int tokenIndex = 0;
//            for (CoreLabel token : sent.get(CoreAnnotations.TokensAnnotation.class)) {
//                ++tokenIndex;
//                tokensStr.add(
//                        String.format("%d\t%s\t%s\t%s\t%s\t_", tokenIndex,
//                                token.get(CoreAnnotations.TextAnnotation.class),
//                                token.get(CoreAnnotations.LemmaAnnotation.class),
//                                token.get(CoreAnnotations.PartOfSpeechAnnotation.class),
//                                token.get(CoreAnnotations.PartOfSpeechAnnotation.class)));
//            }
//            String[] parsedTokens = tokensStr.toArray(new String[0]);
//            parsedTokens = parser_.parseTokens(parsedTokens);
//            List<List<String>> tokensFields = new LinkedList<>();
//            for (String token : parsedTokens) {
//                token = token.replace("complm", "mark").replace("partmod", "vmod")
//                        .replace("purpcl", "vmod").replace("abbrev", "appos")
//                        .replace("infmod", "vmod");
//                tokensFields.add(Arrays.asList(token.split("\t")));
//            }
//            EnglishGrammaticalStructure s =
//                    EnglishGrammaticalStructure.buildCoNLLXGrammaticalStructure(tokensFields);
//            document.get(CoreAnnotations.SentencesAnnotation.class)
//                    .get(sentIndex - 1)
//                    .set(SemanticGraphCoreAnnotations.BasicDependenciesAnnotation.class,
//                            new SemanticGraph(s.typedDependencies()));
//            document.get(CoreAnnotations.SentencesAnnotation.class)
//                    .get(sentIndex - 1)
//                    .set(SemanticGraphCoreAnnotations.CollapsedDependenciesAnnotation.class,
//                            new SemanticGraph(s.typedDependenciesCollapsed()));
//            document.get(CoreAnnotations.SentencesAnnotation.class)
//                    .get(sentIndex - 1)
//                    .set(SemanticGraphCoreAnnotations.CollapsedCCProcessedDependenciesAnnotation.class,
//                            new SemanticGraph(s.typedDependenciesCCprocessed()));
//        }
        return document;
    }
}
