package edu.emory.mathcs.ir.utils

import it.acubelab.tagme._
import it.acubelab.tagme.preprocessing.TopicSearcher

import scala.collection.JavaConverters._

case class EntityMention(start: Int,
                         end: Int,
                         entity: String,
                         rho: Float,
                         coherence: Float,
                         votes: Float)

/**
  * Identifies mentions of Wikipedia entities in text.
  */
object TagMeWikifier {
  val lang = "en"
  it.acubelab.tagme.config.TagmeConfig.init("cfg/config.xml")
  val rel = RelatednessMeasure.create(lang)
  val parser = new TagmeParser(lang, true)
  val disamb = new Disambiguator(lang)
  val segmentation = new Segmentation()
  val rho = new RhoMeasure()
  val searcher = new TopicSearcher(lang)

  def getEntityMentions(text: String): Array[EntityMention] = {
    val annotatedText = new AnnotatedText(text)
    parser.parse(annotatedText)
    segmentation.segment(annotatedText)
    disamb.disambiguate(annotatedText, rel)
    rho.calc(annotatedText, rel)

    annotatedText.getAnnotations.asScala.filter(_.isDisambiguated).map {
      annotation =>
        val start = annotatedText.getOriginalTextStart(annotation)
        val end = annotatedText.getOriginalTextEnd(annotation)
        val score = annotation.getRho
        val coherence = annotation.getCoherence
        val votes = annotation.getVotes
        val entity = searcher.getTitle(annotation.getTopic)
        new EntityMention(start, end, entity, score, coherence, votes)
    }.toArray
  }
}