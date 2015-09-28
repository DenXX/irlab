package edu.emory.mathcs.ir

import java.io.{BufferedWriter, FileWriter, File}
import java.nio.file.{Paths, Files}

import com.typesafe.scalalogging.LazyLogging
import dispatch._, dispatch.Defaults._
import edu.emory.mathcs.ir.web.DocumentDownloader
import scala.pickling.Defaults._, scala.pickling.json._
import scala.pickling.io.TextFileOutput

/**
 * Created by dsavenk on 9/25/15.
 */
object DownloadSerpDocumentsApp extends LazyLogging {

  def parseJsonFile(serializedFile:String): Seq[QuestionSearchResults] = {
    val json = scala.io.Source.fromFile(serializedFile).mkString
    json.unpickle[Seq[QuestionSearchResults]]
  }

  def main(args:Array[String]): Unit = {
    // Directory where to store the downloaded documents.

    val downloadDir = Paths.get(args(1))
    if (!Files.exists(downloadDir)) {
      Files.createDirectory(downloadDir)
    }
    val out = new TextFileOutput(new java.io.File(args(2)))

    var futures = new scala.collection.mutable.ListBuffer[Future[String]]

    // Get serps and download the corresponding documents.
    val serps = parseJsonFile(args(0))
    serps
      .view
      .flatMap { serp =>
        serp.results
          .view.map { result =>
            // Generate temporary filename and if the download succeeds, we
            // store the content of the document in the file with the generated
            // name.
            val fileName = File.createTempFile("doc_", ".txt",
              downloadDir.toFile)
            val downloadedDoc = DocumentDownloader(result.url)
            futures += downloadedDoc

            // What to do if download succeeds.
            downloadedDoc onSuccess {
              case content =>
                val out = new BufferedWriter(new FileWriter(fileName))
                out.write(content)
                out.close()
                logger.info("Successful document download (" + result.url + ")")
            }

            // What to do if it fails
            downloadedDoc onFailure {
              case error => logger.warn("Download error for document (" +
                result.url + "): " + error.getMessage)
            }

            new SerpDocument(serp.question, result.title,
              result.snippet, result.url, fileName.getAbsolutePath)
          }
        .force
      }
      .force
      .pickleTo(out)

    // Terminate the pool after all requests are complete.
    dispatch.Future.sequence(futures) onComplete {
      case _ => DocumentDownloader.close()
    }
  }
}

case class SerpDocument(query:String,
                        title:String,
                        snippet:String,
                        url:String,
                        contentDocument:String)