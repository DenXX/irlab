package edu.emory.mathcs.ir

import edu.emory.mathcs.ir.search.Search

/**
 * Created by dsavenk on 9/18/15.
 */
object KbQaApp {
  def main(args : Array[String]) {
    val results = Search("what is the highest mountain in the southeast USA?")
    results match {
      case Right(res) => println(res)
      case Left(error) => println(error)
    }
  }
}
