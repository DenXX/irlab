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
    val out = new PrintWriter(new GZIPOutputStream(new BufferedOutputStream(new FileOutputStream(args(1)))))
    try {
      var counter = 0
      val start = System.nanoTime()
      for (qna <- input.asScala if qna.questionBody.isEmpty) {
        val question = qna.questionTitle.replace("\n", " ").replace("\t", " ")
        val answer = qna.bestAnswer.replace("\n", " ").replace("\t", " ")
        val fullText = question + "?\t" + answer
        val mentions = TagMeWikifier.getEntityMentions(fullText)
        out.println(fullText)
        out.println(mentions.length)
        mentions.map(mention => mention.entity + "\t" + mention.start + "\t" +
            mention.end + "\t" + fullText.substring(mention.start, mention.end) +
            mention.rho + "\t" + mention.coherence + "\t" + mention.votes)
          .foreach(out.println)
        counter += 1
        if (counter % 100 == 0) println(counter + " questions processed..." + (1e-9 * (System.nanoTime() - start) / counter) + " seconds per questions")
      }
    } finally {
      out.close()
    }
  }

}
