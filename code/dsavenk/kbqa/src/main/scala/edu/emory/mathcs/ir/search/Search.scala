package edu.emory.mathcs.ir.search

import dispatch._, Defaults._
import org.json4s._


/**
 * A class that uses Bing Web Search API to return search results for the given
 * query.
 * @param apiKey An API key to access Bing Search API.
 */
class BingSearch(private val apiKey:String) {
  private val baseUrl = "https://api.datamarket.azure.com/Bing/Search/v1/Web?"
  implicit val formats = DefaultFormats

  def apply(query:String, topN:Int) : Either[Throwable, Seq[SearchResult]] = {
    val parameterMap = getQueryParameterMap(query, topN) + ("$format" -> "json")
    val queryString = parameterMap.map { case (k, v) => k + "=" + v }
      .mkString("&")
    val requestUrl = url(baseUrl + queryString).as_!("", apiKey)
    val http = new Http
    val response = http(requestUrl OK as.json4s.Json)
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
object Search {
  def apply(query:String) : Either[Throwable, Seq[SearchResult]] = {
    // TODO(denxx): Move the actual key to resources.
    val search = new BingSearch("ua4NbbaJUUabS47ZzGM2VANoW3s+EdogrHxbtRRsg1Y")
    search(query, 50)
  }
}

case class SearchResult(rank:Int, url:String,
                        title:String = "", snippet:String = "")