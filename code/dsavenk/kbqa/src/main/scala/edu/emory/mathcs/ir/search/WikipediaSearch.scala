package edu.emory.mathcs.ir.search

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import dispatch._, dispatch.Defaults._
import scala.collection.JavaConverters._
import org.json4s._

case class OpenSearchApiResponse(batchcomplete: String, continue: Option[OpenSearchContinue],
                                 query: OpenSearchQuery)
case class OpenSearchContinue(sroffset: Int, continue:String)
case class OpenSearchQuery(searchinfo: OpenSearchInfo, search: List[OpenSearchResult])
case class OpenSearchInfo(totalhits: Int)
case class OpenSearchResult(ns: Int, title: String, snippet: String, size: Int,
                            wordcount: Int, timestamp: String)

/**
  * Created by dsavenk on 5/3/16.
  */
class WikipediaSearch extends SearchDocuments{
  val baseUrl = "https://en.wikipedia.org/w/api.php?"
  val basePageUrl = "https://en.wikipedia.org/wiki/"
  implicit val formats = DefaultFormats

  override def search(query: String, topN: Int): Either[Throwable, Seq[SearchResult]] = {
    val parameterMap = getQueryParameterMap(query.replace("?", ""), topN)
    val queryString = parameterMap.map { case (k, v) => k + "=" + v }
      .mkString("&")
    val requestUrl = url(baseUrl + queryString)
    val response = Http(requestUrl OK as.json4s.Json)
    val results = response.map(
      json => {
        val response = json.extract[OpenSearchApiResponse]
        response.query.search
          .zipWithIndex
          .map {
            case (result: OpenSearchResult, index: Int) =>
              new SearchResult(
                index, basePageUrl + result.title.replace("\\s", "_"), result.title, result.snippet)
          }
      }
    )
    results.either()
  }

  private def getQueryParameterMap(query: String, topN:Int, offset:Int = 0) =
    Map(
      "srsearch" -> java.net.URLEncoder.encode(query, "UTF8"),
      "sroffset" -> offset,
      "action" -> "query",
      "list" -> "search",
      "format" -> "json"
    )

  def close(): Unit = {
    Http.shutdown()
  }
}

object WikipediaSearchApp extends App {
  val search = new WikipediaSearch
  val results = search.search("when did the king tut die?", 10)
  println(results)
}