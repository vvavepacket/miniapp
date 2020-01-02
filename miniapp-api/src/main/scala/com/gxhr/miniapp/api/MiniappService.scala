package com.gxhr.miniapp.api

import java.time.{Instant, LocalDateTime}

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.api.broker.kafka.{KafkaProperties, PartitionKeyStrategy}
import com.lightbend.lagom.scaladsl.api.{Descriptor, Service, ServiceAcl, ServiceCall}
import play.api.libs.json.{Format, Json}

object MiniappService  {
  val TOPIC_NAME = "greetings"
}

/**
  * The miniapp service interface.
  * <p>
  * This describes everything that Lagom needs to know about how to serve and
  * consume the MiniappService.
  */
trait MiniappService extends Service {

  def hello(id: String): ServiceCall[NotUsed, String]

  def upload(): ServiceCall[UploadMessage, UploadMessageDone]

  def edit(id: String): ServiceCall[EditMessage, Done]

  def uploadNewVersion(id: String): ServiceCall[UploadNewVersionMessage, Done]

  def submitForReview(id: String): ServiceCall[NotUsed, Done]

  def approve(id: String): ServiceCall[NotUsed, Done]

  def reject(id: String): ServiceCall[NotUsed, Done]

  def status(id: String): ServiceCall[NotUsed, Miniapp]

  /**
    * This gets published to Kafka.
    */
  //def greetingsTopic(): Topic[GreetingMessageChanged]

  override final def descriptor: Descriptor = {
    import Service._
    // @formatter:off
    named("miniapp")
      .withCalls(
        //pathCall("/api/hello/:id", hello _),
        //pathCall("/api/hello/:id", useGreeting _)
        pathCall("/miniapp/upload", upload _),
        pathCall("/miniapp/edit/:id", edit _),
        pathCall("/miniapp/uploadNewVersion/:id", uploadNewVersion _),
        pathCall("/miniapp/review/:id", submitForReview _),
        pathCall("/miniapp/approve/:id", approve _),
        pathCall("/miniapp/reject/:id", reject _),
        pathCall("/miniapp/status/:id", status _)
      )
      /*
      .withTopics(
        topic(MiniappService.TOPIC_NAME, greetingsTopic _)
          // Kafka partitions messages, messages within the same partition will
          // be delivered in order, to ensure that all messages for the same user
          // go to the same partition (and hence are delivered in order with respect
          // to that user), we configure a partition key strategy that extracts the
          // name as the partition key.
          .addProperty(
            KafkaProperties.partitionKeyStrategy,
            PartitionKeyStrategy[GreetingMessageChanged](_.name)
          )
      )
       */
      .withAutoAcl(true)
      .withAcls(
        ServiceAcl(pathRegex = Some("/miniapp/uploadFile"))
      )
    // @formatter:on
  }
}

case class UploadMessage(userId: String, name: String, version: String, tags: List[String])

object UploadMessage {
  implicit val format: Format[UploadMessage] = Json.format[UploadMessage]
}

case class EditMessage(name: String, userId: String, tags: List[String])

object EditMessage {
  implicit val format: Format[EditMessage] = Json.format[EditMessage]
}

case class UploadNewVersionMessage(userId: String, name: String, version: String, tags: List[String])

object UploadNewVersionMessage {
  implicit val format: Format[UploadNewVersionMessage] = Json.format[UploadNewVersionMessage]
}

case class UploadMessageDone(id: String)

object UploadMessageDone {
  implicit val format: Format[UploadMessageDone] = Json.format[UploadMessageDone]
}

case class Miniapp(name: String, userId: String, version: String, tags: List[String], createdTS: Instant, status: String, fileName: String)

object Miniapp {
  implicit val format: Format[Miniapp] = Json.format
}