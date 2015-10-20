package edu.emory.mathcs.ir

import dispatch._, dispatch.Defaults._
import scala.concurrent.Future

/**
 * Created by dsavenk on 9/25/15.
 */
object WebDocument {
  def get(documentUrl:String) : Option[String] = {
    getAsync(documentUrl).option()
  }

  def getAsync(documentUrl:String) : Future[String] = {
    Http(url(documentUrl) OK as.String)
  }
}
