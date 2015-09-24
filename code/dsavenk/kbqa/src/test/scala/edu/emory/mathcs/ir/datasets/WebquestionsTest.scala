package edu.emory.mathcs.ir.datasets

import org.scalatest._

class WebquestionsTest extends FlatSpec with Matchers {

  "Parser" should "parse question-answer pairs from Webquestions json" in {
    val input =
      """
        |[
        |  {"url": "http://www.freebase.com/view/en/justin_bieber", "targetValue": "(list (description \"Jazmyn Bieber\") (description \"Jaxon Bieber\"))", "utterance": "what is the name of justin bieber brother?"},
        |  {"url": "http://www.freebase.com/view/en/natalie_portman", "targetValue": "(list (description \"Padm\u00e9 Amidala\"))", "utterance": "what character did natalie portman play in star wars?"},
        |  {"url": "http://www.freebase.com/view/en/selena_gomez", "targetValue": "(list (description \"New York City\"))", "utterance": "what state does selena gomez?"},
        |]
      """.stripMargin
    val qnaPairs = Webquestions.read(scala.io.Source.fromString(input))
    qnaPairs.size should be (3)
    qnaPairs.head.utterance should be ("what is the name of justin bieber " +
      "brother?")
    qnaPairs.head.url should be ("http://www.freebase.com/view/en/" +
      "justin_bieber")
    val answers = qnaPairs.head.answer
    answers.length should be (2)
    answers(0) should be ("Jazmyn Bieber")
    answers(1) should be ("Jaxon Bieber")
    qnaPairs.drop(1).head.utterance should be ("what character did natalie " +
      "portman play in star wars?")
  }

}
