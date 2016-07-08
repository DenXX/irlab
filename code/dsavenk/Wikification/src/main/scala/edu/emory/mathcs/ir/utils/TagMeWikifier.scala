package edu.emory.mathcs.ir.utils

import it.acubelab.tagme.preprocessing.TopicSearcher

import scala.collection.JavaConverters._

/**
  * Identifies mentions of Wikipedia entities in text.
  */
object TagMeWikifier {
  it.acubelab.tagme.config.TagmeConfig.init("cfg/config.xml")
  val annotator = new it.acubelab.tagme.wrapper.Annotator("en")
  val searcher = new TopicSearcher("en");

  def getEntityMentions(text: String): Array[(Int, Int, String, Float, Float, Float)] = {
    val annotatedText = annotator.annotates(text)
    annotatedText.getAnnotations.asScala.filter(_.isDisambiguated).map {
      annotation =>
        val start = annotatedText.getOriginalTextStart(annotation)
        val end = annotatedText.getOriginalTextStart(annotation)
        val score = annotation.getRho
        val coherence = annotation.getCoherence
        val votes = annotation.getVotes
        val entity = searcher.getTitle(annotation.getTopic)
        (start, end, entity, score, coherence, votes)
    }.toArray
  }
}
