package edu.emory.mathcs.ir.search

import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import dispatch._, dispatch.Defaults._
import scala.collection.JavaConverters._
import org.json4s._

trait SearchDocuments {
  def search(query: String, topN: Int): Either[Throwable, Seq[SearchResult]]
}

/**
 * A class that uses Bing Web Search API to return search results for the given
 * query.
 * @param apiKey An API key to access Bing Search API.
 */
class BingSearch(private val apiKey:String) extends SearchDocuments {
  private val baseUrl = "https://api.datamarket.azure.com/Bing/Search/v1/Web?"
  implicit val formats = DefaultFormats

  def search(query:String, topN:Int) : Either[Throwable, Seq[SearchResult]] = {
    val parameterMap = getQueryParameterMap(query, topN) + ("$format" -> "json")
    val queryString = parameterMap.map { case (k, v) => k + "=" + v }
      .mkString("&")
    val requestUrl = url(baseUrl + queryString).as_!("", apiKey)
    val response = Http(requestUrl OK as.json4s.Json)
    val results = response.map(
      json => {
        val response = json.extract[BingResponse]
        response.d.results
          .zipWithIndex
          .map {
            case (result: BingResult, index: Int) =>
              new SearchResult(
                index, result.Url, result.Title, result.Description)
          }
      }
    )
    results.either()
  }

  def close(): Unit = {
    Http.shutdown()
  }

  private def getQueryParameterMap(query: String, topN:Int, offset:Int = 0) =
    Map(
      "Query" -> java.net.URLEncoder.encode("'" + query + "'", "UTF8"),
      "$top" -> topN,
      "$skip" -> offset
    )
}

case class BingResponse(d: BingResponsePage)
case class BingResponsePage(results: List[BingResult], __next: String)
case class BingResult(__metadata: BingMetadata, ID: String, Title: String,
                      Description: String, DisplayUrl: String, Url: String)
case class BingMetadata(uri: String, `type`: String)

/**
 * An object to perform a search over some collection, e.g. Web.
 */
object Search extends LazyLogging {
  private val apiKeys =
    ConfigFactory.load().getStringList("BING_API_KEYS").asScala.toArray
  private var currentKey = 0
  private var search = updateSearch()

  /**
   * Returns search results for the given query.
   * @param query Search query.
   * @param retryNumber The current retry attempt number. By default it is 0.
   * @return Search results. Search can throw an exception, that's why
   *         the method actually returns Either.
   */
  def apply(query:String, retryNumber:Int = 0)
      : Either[Throwable, Seq[SearchResult]] = {
    val result = search.search(query, 50)
    // Search can fail due to request limit exceeded. In this case try to switch
    // API keys and redo.
    if (result.isLeft && retryNumber < apiKeys.length) {
      logger.warn(result.left.get.getMessage)
      currentKey = (currentKey + 1) % apiKeys.length
      search = updateSearch()
      apply(query, retryNumber + 1)
    } else result
  }

  def close(): Unit = {
    search.close()
  }

  private def updateSearch(): BingSearch = {
    //new BingSearch(apiKeys(currentKey))
    new WikipediaSearch
  }
}

case class SearchResult(rank:Int, url:String,
                        title:String = "", snippet:String = "")