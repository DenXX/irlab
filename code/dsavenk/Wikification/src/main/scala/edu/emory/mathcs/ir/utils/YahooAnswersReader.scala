package edu.emory.mathcs.ir.utils

import edu.emory.mathcs.ir.input.YahooWebScopeXmlInput
import scala.collection.JavaConverters._

/**
  * Created by dsavenk on 7/8/16.
  */
object YahooAnswersReader {

  def main(args: Array[String]): Unit = {
    val input = new YahooWebScopeXmlInput("/home/dsavenk/Projects/octiron/data/YahooWebScope/L6-ComprehensiveQuestions/FullOct2007.xml.bz2")
    for (qna <- input.asScala) {
      val question = qna.questionTitle + "\n" + qna.questionBody
      val answer = qna.bestAnswer
      val fullText = question.replace("\n", " ").replace("\t", " ") + "?\t" + answer.replace("\n", " ").replace("\t", " ")
      print(fullText)
      val mentions = TagMeWikifier.getEntityMentions(fullText)
      mentions.foreach(mention => print("\t" + mention.productIterator.toList.mkString("\t")))
      println()
    }
  }

}
