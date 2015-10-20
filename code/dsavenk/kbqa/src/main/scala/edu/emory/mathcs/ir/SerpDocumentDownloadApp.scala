package edu.emory.mathcs.ir

import scala.pickling.Defaults._, scala.pickling.json._

/**
 * Created by dsavenk on 9/25/15.
 */
object SerpDocumentDownloadApp {

  def parseJsonFile(serializedFile:String): Unit = {
    val json = scala.io.Source.fromFile(serializedFile).mkString
    val results = json.unpickle[Seq[QuestionSearchResults]]
    println(results)
  }

  def main(args:Array[String]): Unit = {

  }
}
