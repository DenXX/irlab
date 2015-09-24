package edu.emory.mathcs.ir.datasets

import scala.io.Source

import org.json4s._
import org.json4s.native.JsonMethods._

/**
 * Manipulates input data from the WebQuestions dataset.
 */
object Webquestions {
  implicit private val formats = DefaultFormats

  /**
   * Reads source data in json format and returns parsed records.
   * @param source A source with the input json data.
   * @return A sequence of records read from the input source.
   */
  def read(source: Source) : Seq[WebquestionsRecord] = {
    val json = source.getLines().mkString
    parse(json).extract[List[WebquestionsRecord]]
  }
}

/**
 * Represents a record from an input file of the WebQuestions dataset.
 * @param url Url of the target entity profile webpage.
 * @param targetValue A list of correct answers to the question.
 * @param utterance A natural language question.
 */
case class WebquestionsRecord(url:String,
                              targetValue:String,
                              utterance:String) {
  /**
   * Stores an array of answers to the given questions. The answers are
   * extracted from the targetValue field.
   */
  val answer = decodeTargetValue(targetValue)

  /**
   * Decodes answers stored in LISP format, e.g.
   * "(list (description \"Answer1\") ) and returns simply the list of answers.
   * @param targetValue A source LISP representation, stored in targetValue
   *                    record.
   * @return An array of answers extracted from the LISP representation.
   */
  private def decodeTargetValue(targetValue:String) : Array[String] = {
    targetValue.stripPrefix("(list ").stripSuffix(")").split("\\(description")
      .map(answer => answer
          .replaceAll("^\\s*\"?", "")
          .replaceAll("\\s*\"?\\s*\\)?\\s*$", ""))
      .filter(answer => !answer.isEmpty)
  }
}