package com.gxhr.miniapp.impl

import java.time.Instant

import akka.Done
import com.gxhr.miniapp.api.UploadMessageDone
import com.lightbend.lagom.scaladsl.persistence.{AggregateEvent, AggregateEventTag, PersistentEntity}
import com.lightbend.lagom.scaladsl.persistence.PersistentEntity.ReplyType
import com.lightbend.lagom.scaladsl.playjson.{JsonMigration, JsonSerializer, JsonSerializerRegistry}
import play.api.libs.json._

import scala.collection.immutable.Seq

/**
  * This is an event sourced entity. It has a state, [[MiniappState]], which
  * stores what the greeting should be (eg, "Hello").
  *
  * Event sourced entities are interacted with by sending them commands. This
  * entity supports two commands, a [[UseGreetingMessage]] command, which is
  * used to change the greeting, and a [[Hello]] command, which is a read
  * only command which returns a greeting to the name specified by the command.
  *
  * Commands get translated to events, and it's the events that get persisted by
  * the entity. Each event will have an event handler registered for it, and an
  * event handler simply applies an event to the current state. This will be done
  * when the event is first created, and it will also be done when the entity is
  * loaded from the database - each event will be replayed to recreate the state
  * of the entity.
  *
  * This entity defines one event, the [[GreetingMessageChanged]] event,
  * which is emitted when a [[UseGreetingMessage]] command is received.
  */
class MiniappEntity extends PersistentEntity {

  override type Command = MiniappCommand[_]
  override type Event = MiniappEvent
  override type State = MiniappState

  /**
    * The initial state. This is used if there is no snapshotted state to be found.
    */
  override def initialState: MiniappState = MiniappState("", "", "0.1.0", List[String](), Instant.now(), "New", "")

  /**
    * An entity can define different behaviours for different states, so the behaviour
    * is a function of the current state to a set of actions.
    */
  override def behavior: Behavior = {
    Actions().onCommand[Upload, UploadMessageDone] {

      // Command handler for the UseGreetingMessage command
      case (Upload(name, userId, version, tags), ctx, state) =>
        ctx.thenPersist(
          Uploaded(name, userId, version, tags, Instant.now())
        ) { _ =>
          // Then once the event is successfully persisted, we respond with done.
          ctx.reply(UploadMessageDone(entityId))
        }
    }.onCommand[Edit, Done] {
      case (Edit(name, userId, tags), ctx, state) =>
        ctx.thenPersist(
          Edited(name, userId, tags, Instant.now())
        ) { _ =>
          ctx.reply(Done)
        }
    }.onCommand[UploadNewVersion, Done] {
      case (UploadNewVersion(userId, name, version, tags), ctx, state) =>
        ctx.thenPersist(
          UploadedNewVersion(userId, name, version, tags, Instant.now(), "Uploaded New Version")
        ) { _ =>
          ctx.reply(Done)
        }
    }.onCommand[SubmitForReview, Done] {
      case (SubmitForReview(id), ctx, state) =>
        ctx.thenPersist(
          SubmittedForReview(Instant.now())
        ) { _ =>
          ctx.reply(Done)
        }
    }.onCommand[Approve, Done] {
      case (Approve(id), ctx, state) =>
        ctx.thenPersist(
          Approved(Instant.now())
        ) { _ =>
          ctx.reply(Done)
        }
    }.onCommand[Reject, Done] {
      case (Reject(id), ctx, state) =>
        ctx.thenPersist(
          Rejected(Instant.now())
        ) { _ =>
          ctx.reply(Done)
        }
    }.onCommand[UploadMiniappFile, Done] {
      case (UploadMiniappFile(fileName), ctx, state) =>
        ctx.thenPersist(
          UploadedMiniappFile(fileName)
        ) { _ =>
          ctx.reply(Done)
        }
    }.onReadOnlyCommand[Status, MiniappState] {
      case (Status(id), ctx, state) =>
        if (state.name == "" && state.userId == "")
          ctx.commandFailed(MiniappException("Entity not found"))
        else
          ctx.reply(state)

    }.onEvent {
      // Event handler for the GreetingMessageChanged event
      case (Uploaded(name, userId, version, tags, createdTS), state) =>
        state.copy(name, userId, version, tags, createdTS)
      case (Edited(name, userId, tags, _), state) =>
        state.copy(name = name, userId = userId, tags = tags)
      case (UploadedNewVersion(userId, name, version, tags, _, status), state) =>
        state.copy(userId = userId, name = name, version = version, tags = tags, status = status)
      case (SubmittedForReview(_), state) =>
        state.copy(status = "submitted")
      case (Approved(_), state) =>
        state.copy(status = "approved")
      case (Rejected(_), state) =>
        state.copy(status = "rejected")
      case (UploadedMiniappFile(fileName), state) =>
        state.copy(fileName = fileName)
    }
  }
}

/**
  * The current state held by the persistent entity.
  */
case class MiniappState(
                         name: String,
                         userId: String,
                         version: String,
                         tags: List[String],
                         createdTS: Instant,
                         status: String,
                         fileName: String
                       )

object MiniappState {
  /**
    * Format for the hello state.
    *
    * Persisted entities get snapshotted every configured number of events. This
    * means the state gets stored to the database, so that when the entity gets
    * loaded, you don't need to replay all the events, just the ones since the
    * snapshot. Hence, a JSON format needs to be declared so that it can be
    * serialized and deserialized when storing to and from the database.
    */
  implicit val format: Format[MiniappState] = Json.format
}

/**
  * This interface defines all the events that the MiniappEntity supports.
  */
sealed trait MiniappEvent extends AggregateEvent[MiniappEvent] {
  def aggregateTag: AggregateEventTag[MiniappEvent] = MiniappEvent.Tag
}

object MiniappEvent {
  val Tag: AggregateEventTag[MiniappEvent] = AggregateEventTag[MiniappEvent]
}

//event
case class Uploaded(name: String, userId: String, version: String, tags: List[String], createdTS: Instant) extends MiniappEvent

object Uploaded {
  implicit val format: Format[Uploaded] = Json.format
}

case class Edited(name: String, userId: String, tags: List[String], eventTime: Instant) extends MiniappEvent

object Edited {
  implicit val format: Format[Edited] = Json.format
}

case class UploadedNewVersion(userId: String, name: String, version: String, tags: List[String], eventTime: Instant, status: String) extends MiniappEvent

object UploadedNewVersion {
  implicit val format: Format[UploadedNewVersion] = Json.format
}

case class SubmittedForReview(eventTime: Instant) extends MiniappEvent

object SubmittedForReview {
  implicit val format: Format[SubmittedForReview] = Json.format
}

case class Approved(eventTime: Instant) extends MiniappEvent

object Approved {
  implicit val format: Format[Approved] = Json.format
}

case class Rejected(eventTime: Instant) extends MiniappEvent

object Rejected {
  implicit val format: Format[Rejected] = Json.format
}

case class UploadedMiniappFile(fileName: String) extends MiniappEvent

object UploadedMiniappFile {
  implicit val format: Format[UploadedMiniappFile] = Json.format
}

/**
  * An event that represents a change in greeting message.
  */
case class GreetingMessageChanged(message: String) extends MiniappEvent

object GreetingMessageChanged {

  /**
    * Format for the greeting message changed event.
    *
    * Events get stored and loaded from the database, hence a JSON format
    * needs to be declared so that they can be serialized and deserialized.
    */
  implicit val format: Format[GreetingMessageChanged] = Json.format
}

/**
  * This interface defines all the commands that the MiniappEntity supports.
  */
sealed trait MiniappCommand[R] extends ReplyType[R]

// commands
case class Upload(name: String, userId: String, version: String, tags: List[String]) extends MiniappCommand[UploadMessageDone]

object Upload {
  implicit val format: Format[Upload] = Json.format
}

case class Edit(name: String, userId: String, tags: List[String]) extends MiniappCommand[Done]

object Edit {
  implicit val format: Format[Edit] = Json.format
}

case class UploadNewVersion(userId: String, name: String, version: String, tags: List[String]) extends MiniappCommand[Done]

object UploadNewVersion {
  implicit val format: Format[UploadNewVersion] = Json.format
}

case class SubmitForReview(id: String) extends MiniappCommand[Done]

object SubmitForReview {
  implicit val format: Format[SubmitForReview] = Json.format
}

case class Approve(id: String) extends MiniappCommand[Done]

object Approve {
  implicit val format: Format[Approve] = Json.format
}

case class Reject(id: String) extends MiniappCommand[Done]

object Reject {
  implicit val format: Format[Reject] = Json.format
}

case class Status(id: String) extends MiniappCommand[MiniappState]

object Status {
  implicit val format: Format[Status] = Json.format
}

case class UploadMiniappFile(fileName: String) extends MiniappCommand[Done]

object UploadMiniappFile {
  implicit val format: Format[UploadMiniappFile] = Json.format
}

/**
  * A command to switch the greeting message.
  *
  * It has a reply type of [[Done]], which is sent back to the caller
  * when all the events emitted by this command are successfully persisted.
  */
case class UseGreetingMessage(message: String) extends MiniappCommand[Done]

object UseGreetingMessage {

  /**
    * Format for the use greeting message command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[UseGreetingMessage] = Json.format
}

/**
  * A command to say hello to someone using the current greeting message.
  *
  * The reply type is String, and will contain the message to say to that
  * person.
  */
case class Hello(name: String) extends MiniappCommand[String]

object Hello {

  /**
    * Format for the hello command.
    *
    * Persistent entities get sharded across the cluster. This means commands
    * may be sent over the network to the node where the entity lives if the
    * entity is not on the same node that the command was issued from. To do
    * that, a JSON format needs to be declared so the command can be serialized
    * and deserialized.
    */
  implicit val format: Format[Hello] = Json.format
}

case class MiniappException(message: String) extends RuntimeException(message)

object MiniappException {
  implicit val format: Format[MiniappException] = Json.format[MiniappException]
}
/**
  * Akka serialization, used by both persistence and remoting, needs to have
  * serializers registered for every type serialized or deserialized. While it's
  * possible to use any serializer you want for Akka messages, out of the box
  * Lagom provides support for JSON, via this registry abstraction.
  *
  * The serializers are registered here, and then provided to Lagom in the
  * application loader.
  */
object MiniappSerializerRegistry extends JsonSerializerRegistry {
  override def serializers: Seq[JsonSerializer[_]] = Seq(
    JsonSerializer[UseGreetingMessage],
    JsonSerializer[Hello],
    JsonSerializer[GreetingMessageChanged],
    JsonSerializer[MiniappState],
    JsonSerializer[MiniappException],
    // response
    JsonSerializer[UploadMessageDone],
    // command
    JsonSerializer[Upload],
    JsonSerializer[Edit],
    JsonSerializer[UploadNewVersion],
    JsonSerializer[SubmitForReview],
    JsonSerializer[Approve],
    JsonSerializer[Reject],
    JsonSerializer[Status],
    JsonSerializer[UploadMiniappFile],
    // event
    JsonSerializer[Uploaded],
    JsonSerializer[Edited],
    JsonSerializer[UploadedNewVersion],
    JsonSerializer[SubmittedForReview],
    JsonSerializer[Approved],
    JsonSerializer[Rejected],
    JsonSerializer[UploadedMiniappFile]
  )

  /*
  private val test1AddedMigration = new JsonMigration(2) {
    override def transform(fromVersion: Int, json: JsObject): JsObject = {
      if (fromVersion < 2) {
        json + ("test1" -> JsString("sample"))
      } else {
        json
      }
    }
  }
   */

  /*
  override def migrations = Map[String, JsonMigration](
    classOf[UploadedNewVersion].getName -> test1AddedMigration
  )
   */

}
