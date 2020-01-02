package com.gxhr.miniapp.impl

import java.io.File
import java.util.UUID

import akka.stream.IOResult
import akka.stream.scaladsl.{FileIO, Sink}
import akka.util.ByteString
import play.api.libs.streams.Accumulator
import play.api.mvc.DefaultActionBuilder
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc.{DefaultActionBuilder, Results}
import play.api.routing.Router
import play.api.routing.sird._
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}
import java.io.File
import java.util.UUID

import akka.stream.scaladsl.{FileIO, Sink}
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import com.lightbend.lagom.scaladsl.persistence.PersistentEntityRegistry
import play.api.Logger
import play.api.libs.streams.Accumulator
import play.api.mvc.MultipartFormData.FilePart
import play.api.mvc._
import play.core.parsers.Multipart.{FileInfo, FilePartHandler}

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.Future

class FileUploadRouter(action: DefaultActionBuilder,
                       parser: PlayBodyParsers,
                       implicit val exCtx: ExecutionContext,
                       persistentEntityRegistry: PersistentEntityRegistry) {

  // A Play FilePartHandler[T] creates an Accumulator (similar to Akka Stream's Sinks)
  // for each FileInfo in the multipart request.
  private def fileHandler: FilePartHandler[File] = {
    case FileInfo(partName, filename, contentType, _) =>
      val tempFile = {
        println(persistentEntityRegistry)
        // create a temp file in the `target` folder
        val f = new java.io.File("./target/file-upload-data/uploads", UUID.randomUUID().toString).getAbsoluteFile
        // make sure the sub-folders inside `target` exist.
        f.getParentFile.mkdirs()
        //println(f.getAbsolutePath)
        f
      }
      val sink: Sink[ByteString, Future[IOResult]] = FileIO.toPath(tempFile.toPath)
      val acc: Accumulator[ByteString, IOResult] = Accumulator(sink)
      acc.map {
        case akka.stream.IOResult(_, _) =>
          FilePart(partName, filename, contentType, tempFile)
      }
  }

  val router = Router.from {
    case POST(p"/miniapp/uploadFile") =>
      action(parser.multipartFormData(fileHandler)) { request =>
        val files = request.body.files.map(_.ref.getAbsolutePath)
        val id = request.body.dataParts.get("id");
        val q = files(0).split("/")
        val fileUUID = q(q.length-1)
        // send command uploadfile
        persistentEntityRegistry.refFor[MiniappEntity](id.get(0)).ask(UploadMiniappFile(fileUUID))
        Results.Ok(files.mkString("Uploaded[", ", ", "]"))
      }
  }
}
