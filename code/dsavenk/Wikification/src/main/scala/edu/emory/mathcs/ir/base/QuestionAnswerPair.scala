package edu.emory.mathcs.ir.base

import scala.collection.JavaConverters._

/**
  * Created by dsavenk on 7/8/16.
  */
class QuestionAnswerPair(val id: String,
                         val questionTitle: String,
                         val questionBody: String,
                         val categories: Array[String],
                         val bestAnswer: String,
                         attrs: java.util.Map[String, String]) {
  val attributes = attrs.asScala
}