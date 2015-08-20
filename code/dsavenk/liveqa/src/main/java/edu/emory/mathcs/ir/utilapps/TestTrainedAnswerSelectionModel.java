package edu.emory.mathcs.ir.utilapps;

import edu.emory.mathcs.ir.qa.Answer;
import edu.emory.mathcs.ir.qa.Question;
import edu.emory.mathcs.ir.qa.answerer.passage.SentenceBasedPassageRetrieval;
import edu.emory.mathcs.ir.qa.answerer.query.TitleOnlyQueryFormulator;
import edu.emory.mathcs.ir.qa.answerer.ranking.FeatureBasedAnswerSelector;
import edu.emory.mathcs.ir.qa.answerer.web.WebSearchBasedAnswerer;
import edu.emory.mathcs.ir.qa.ml.*;
import edu.emory.mathcs.ir.search.BingWebSearch;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.FileSystems;

/**
 * Created by dsavenk on 8/20/15.
 */
public class TestTrainedAnswerSelectionModel {

    public static void main(String[] args) throws IOException {
        final String modelPath = args[1];

        final Directory directory = FSDirectory.open(
                FileSystems.getDefault().getPath(args[0]));
        final IndexReader indexReader = DirectoryReader.open(directory);
        final FeatureGeneration featureGenerator =
                new CombinerFeatureGenerator(
                        new LemmaPairsFeatureGenerator(),
                        new BM25FeatureGenerator(indexReader),
                        new NamedEntityTypesFeatureGenerator());
        final WebSearchBasedAnswerer answerer =
                new WebSearchBasedAnswerer(new TitleOnlyQueryFormulator(),
                        new FeatureBasedAnswerSelector(modelPath,
                                featureGenerator),
                        new BingWebSearch(),
                        new SentenceBasedPassageRetrieval());
        final Question q = new Question(
                "", " What type of sugar can you use if diabetic?", "", "General Knowledge");
        final Answer answer = answerer.GetAnswer(q);
        System.out.println(answer.toString());
    }
}
