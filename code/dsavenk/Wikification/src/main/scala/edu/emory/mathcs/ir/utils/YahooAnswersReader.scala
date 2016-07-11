package edu.emory.mathcs.ir.utils

import java.io.{BufferedOutputStream, FileOutputStream, PrintWriter}
import java.util.zip.GZIPOutputStream

import edu.emory.mathcs.ir.input.YahooWebScopeXmlInput
import scala.collection.JavaConverters._

/**
  * Created by dsavenk on 7/8/16.
  */
object YahooAnswersReader {

  def main(args: Array[String]): Unit = {
    val input = new YahooWebScopeXmlInput(args(0))
    val out = new PrintWriter(new BufferedOutputStream(new FileOutputStream(args(1))))  
    try {
      for (qna <- input.asScala) {
        val question = (qna.questionTitle + "\n" + qna.questionBody).replace("\n", " ").replace("\t", " ")
        val answer = qna.bestAnswer.replace("\n", " ").replace("\t", " ")
        val fullText = question + "?\t" + answer
        val mentions = TagMeWikifier.getEntityMentions(fullText)
        out.println(fullText)
        out.println(mentions.length)
        mentions.map(mention => mention.entity + "\t" + mention.start + "\t" +
            mention.end + "\t" + mention.rho + "\t" + mention.coherence + "\t" +
            mention.votes)
          .foreach(println(_))
      }
    } finally {
      out.close()
    }
  }

}
