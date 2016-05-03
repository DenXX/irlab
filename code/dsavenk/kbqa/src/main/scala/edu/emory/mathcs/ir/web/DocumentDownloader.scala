package edu.emory.mathcs.ir.web

import java.net.URLEncoder

import com.ning.http.client.extra.ThrottleRequestFilter
import com.typesafe.config.ConfigFactory
import dispatch._
import dispatch.Defaults._

import scala.concurrent.Future

/**
 * Created by dsavenk on 9/28/15.
 */
object DocumentDownloader {
  val userAgent = ConfigFactory.load().getString("USER_AGENT")
  val http = Http.configure(_ setFollowRedirect true)
    .configure(_.addRequestFilter(new ThrottleRequestFilter(100)))

  /**
   * Requests a download of a document with the provided URL and returns a
   * future.
   * @param documentUrl Url of the document to download.
   * @return A future with the content of the document (unless a failure occur).
   */
  def apply(documentUrl:String) : Future[String] = {
    try {
      val request = url(documentUrl.replace("https:", "http:")) <:<
        Map("User-Agent" -> userAgent)
      http(request OK as.String)
    } catch {
      case exc:Throwable => Future.failed(exc)
    }
  }

  def close(): Unit = {
    http.shutdown()
    Http.shutdown()
  }
}
