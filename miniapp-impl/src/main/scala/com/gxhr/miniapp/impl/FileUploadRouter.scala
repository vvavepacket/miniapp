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
import java.util.zip.ZipInputStream

import akka.stream.scaladsl.{FileIO, Sink}
import akka.stream.{IOResult, Materializer}
import akka.util.ByteString
import com.amazonaws.auth.{AWSStaticCredentialsProvider, BasicAWSCredentials}
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.model.{BucketWebsiteConfiguration, PutObjectRequest}
import com.amazonaws.services.s3.{AmazonS3, AmazonS3ClientBuilder}
import com.amazonaws.{AmazonServiceException, SdkClientException}
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
        val f = new java.io.File("/tmp/miniapp-uploads", UUID.randomUUID().toString).getAbsoluteFile
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
    case POST(p"/uploadFile") =>
      action(parser.multipartFormData(fileHandler)) { request =>
        //val files = request.body.files.map(_.ref.getAbsolutePath)
        val id = request.body.dataParts.get("miniapp_id").get.mkString;
        val versionKey = request.body.dataParts.get("versionKey").get.mkString
        val dist = request.body.files.filter(_.key == "dist").head.ref.getAbsolutePath
        val sourceCode = request.body.files.filter(_.key == "sourceCode").head.ref.getAbsolutePath
        miniappWebsiteBucketName = id + "." + domainName
        // upload to s3
        uploadToS3(miniappZipBucketName, id + File.separator + versionKey + File.separator + "dist.zip", dist)
        uploadToS3(miniappZipBucketName, id + File.separator + versionKey + File.separator + "sourceCode.zip", sourceCode)

        // deployToS3
        deployToS3(id, dist)

        // send command uploadfile
        persistentEntityRegistry.refFor[MiniappEntity](id).ask(UploadMiniappFile(id))
        Results.Ok("Uploaded")
      }
  }

  val clientRegion = Regions.AP_SOUTHEAST_1
  val miniappZipBucketName = sys.env("MINIAPP_ZIP_BUCKET_NAME")
  val accessKey = sys.env("ACCESS_KEY")
  val secretKey = sys.env("SECRET_KEY")
  val domainName = sys.env("DOMAIN_NAME")
  var miniappWebsiteBucketName = ""

  private def uploadToS3(bucketName: String, objectName: String, path: String) = {
    try {
      val awsCreds = new BasicAWSCredentials(accessKey, secretKey)
      val s3Client: AmazonS3  = AmazonS3ClientBuilder.standard()
        .withRegion(clientRegion)
        .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
        .build();

      val request = new PutObjectRequest(bucketName, objectName , new File(path))
      s3Client.putObject(request)

    } catch {
      case x: AmazonServiceException => {
        x.printStackTrace()
      }
      case x: SdkClientException => {
        x.printStackTrace()
      }
    }
  }

  private def deployToS3(id: String, zipFile: String) = {
    try {
      val awsCreds = new BasicAWSCredentials(accessKey, secretKey)
      val s3Client: AmazonS3  = AmazonS3ClientBuilder.standard()
        .withRegion(clientRegion)
        .withCredentials(new AWSStaticCredentialsProvider(awsCreds))
        .build();

      val newBucketName: String = id + "." + domainName

      // check if bucket does not exist.. if does not, create it
      if (!s3Client.doesBucketExistV2(newBucketName)) {
        import com.amazonaws.services.s3.model.CreateBucketRequest
        s3Client.createBucket(new CreateBucketRequest(newBucketName))
        s3Client.setBucketWebsiteConfiguration(newBucketName, new BucketWebsiteConfiguration("index.html"))
        s3Client.setBucketPolicy(newBucketName, "{\n    \"Version\": \"2012-10-17\",\n    \"Statement\": [\n        {\n            \"Sid\": \"PublicReadGetObject\",\n            \"Effect\": \"Allow\",\n            \"Principal\": \"*\",\n            \"Action\": \"s3:GetObject\",\n            \"Resource\": \"arn:aws:s3:::"+newBucketName+"/*\"\n        }\n    ]\n}")
      }
      // check if bucket exists, if it exists, empty it...
      else {
        var objectListing = s3Client.listObjects(newBucketName)
        import scala.util.control.Breaks._
        breakable {
          val objIter = objectListing.getObjectSummaries.iterator
          while (objIter.hasNext()) {
            s3Client.deleteObject(newBucketName, objIter.next.getKey)
          }
          // If the bucket contains many objects, the listObjects() call
          // might not return all of the objects in the first listing. Check to
          // see whether the listing was truncated. If so, retrieve the next page of objects
          // and delete them.
          if (objectListing.isTruncated()) {
            objectListing = s3Client.listNextBatchOfObjects(objectListing)
          } else {
            break
          }
        }
      }

      // unzip the file temporarily and upload them
      unzip(zipFile, "/tmp/miniapp-uploads/" + UUID.randomUUID().toString)


    } catch {
      case x: AmazonServiceException => {
        x.printStackTrace()
      }
      case x: SdkClientException => {
        x.printStackTrace()
      }
    }
  }

  private def unzip(zipFilePath: String, destDirectory: String): Unit = {
    import java.io.FileInputStream
    import java.util.zip.ZipEntry
    import java.util.zip.ZipInputStream
    val destDir = new File(destDirectory)
    if (!destDir.exists) destDir.mkdir
    val zipIn = new ZipInputStream(new FileInputStream(zipFilePath))
    var entry = zipIn.getNextEntry
    // iterates over entries in the zip file...
    while ( {
      entry != null
    }) {
      val filePath = destDirectory + File.separator + entry.getName
      if (!entry.isDirectory) { // if the entry is a file, extracts it
        extractFile(zipIn, filePath)
        // upload to s3
        uploadToS3(miniappWebsiteBucketName, entry.getName, filePath)
      }
      else { // if the entry is a directory, make the directory
        val dir = new File(filePath)
        dir.mkdir
      }
      zipIn.closeEntry()
      entry = zipIn.getNextEntry
    }
  }

  private def extractFile(zipIn: ZipInputStream , filePath: String): Unit = {
    import java.io.BufferedOutputStream
    import java.io.FileOutputStream
    val bos = new BufferedOutputStream(new FileOutputStream(filePath))
    val bytesIn = new Array[Byte](4096)
    var read = 0
    read = zipIn.read(bytesIn)
    while (read != -1) {
      bos.write(bytesIn, 0, read)
      read = zipIn.read(bytesIn)
    }
    bos.close
  }
}
