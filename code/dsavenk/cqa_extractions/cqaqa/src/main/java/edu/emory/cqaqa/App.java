package edu.emory.cqaqa;

import edu.emory.cqaqa.parser.YAnswersXmlParser;
import edu.emory.cqaqa.processor.*;

/**
 * Hello world!
 */
public class App 
{
    public static void main( String[] args ) {
        ProcessorPipeline pipeline = new ProcessorPipeline();
        try {
            pipeline.addProcessor(new LanguageFilterProcessor("en-us"));
            pipeline.addProcessor(new FilterMultipleQuestionsProcessor());
            pipeline.addProcessor(new EntityLinkerProcessor());
            pipeline.addProcessor(new FilterNotLinkedQAProcessor());
            pipeline.addProcessor(new FilterMultipleEntitiesQuestionsProcessor());
//            pipeline.addProcessor(new LinkPredicateProcessor("/home/dsavenk/ir/data/Freebase/freebase-rdf-2014-04-13-00-00.gz"));
//            pipeline.addProcessor(new LinkPredicateProcessor("/home/dsavenk/Projects/octiron/data/Freebase/freebase-rdf-2014-04-13-00-00.gz"));
            EntityPairCollectorProcessor p = new EntityPairCollectorProcessor();
            pipeline.addProcessor(p);
            YAnswersXmlParser.parse(args[0], pipeline);
            p.printPairs();
//            List<List<HasWord>> templates = entityTaggerProcessor.getQuestionTemplates();
        } catch (Exception e) {
            e.printStackTrace();
        }



    }
}
