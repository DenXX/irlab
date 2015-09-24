package edu.emory.mathcs.ir

import java.nio.file.FileSystems

import com.typesafe.scalalogging.LazyLogging
import dispatch.Http
import edu.emory.mathcs.ir.datasets.Webquestions
import edu.emory.mathcs.ir.search._
import scala.pickling.Defaults._, scala.pickling.json._
import scala.pickling.io.TextFileOutput

case class QuestionSearchResults(question:String, results:Seq[SearchResult])

/**
 * Read the questions from the WebQuestions dataset and calls Bing Web Search
 * API for the results.
 */
object BingSearchScraperApp extends LazyLogging {
  def scrapeSearchResults(webQuestionsFile:String,
                          output:TextFileOutput): Unit = {
    Webquestions.read(scala.io.Source.fromFile(webQuestionsFile))
      .view
      .map(qna => (qna.utterance, Search(qna.utterance)))
      .filter(questionSearch => questionSearch._2.isRight)
      .map(questionSearch => {
          logger.info("Processing: " + questionSearch._1)
          new QuestionSearchResults(questionSearch._1,
            questionSearch._2.right.get) })
      .force
      .pickleTo(output)
  }

  def parseJsonFile(serializedFile:String): Unit = {
    val json = scala.io.Source.fromFile(serializedFile).mkString
    val results = json.unpickle[Seq[QuestionSearchResults]]
    println(results)
  }

  def main(args: Array[String]): Unit = {
    val out = new TextFileOutput(FileSystems.getDefault.getPath(args(1)).toFile)
    try {
      scrapeSearchResults(args(0), out)
    } finally {
      out.close()
    }
    Search.close()
  }
}
