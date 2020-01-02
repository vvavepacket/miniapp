package com.gxhr.miniapp.impl

import java.util.UUID

import akka.{Done, NotUsed}
import com.gxhr.miniapp.api
import com.gxhr.miniapp.api.{Miniapp, MiniappService, UploadNewVersionMessage}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.lightbend.lagom.scaladsl.api.broker.Topic
import com.lightbend.lagom.scaladsl.broker.TopicProducer
import com.lightbend.lagom.scaladsl.persistence.{EventStreamElement, PersistentEntityRegistry}
import scala.concurrent.ExecutionContext.Implicits.global

/**
  * Implementation of the MiniappService.
  */
class MiniappServiceImpl(persistentEntityRegistry: PersistentEntityRegistry) extends MiniappService {

  private def entityRef(id: String) =
    persistentEntityRegistry.refFor[MiniappEntity](id)

  override def upload() = ServiceCall { request =>
    // generate uuid
    val id = UUID.randomUUID().toString
    entityRef(id)
      .ask(Upload(request.name, request.userId, request.version, request.tags))
  }

  override def edit(id: String) = ServiceCall { request =>
    entityRef(id)
      .ask(Edit(request.name, request.userId, request.tags))
  }

  override def status(id: String) = ServiceCall { _ =>
    entityRef(id)
      .ask(Status())
      .map(state => Miniapp(state.name, state.userId, state.version, state.tags, state.createdTS, state.status, state.fileName))
  }

  override def uploadNewVersion(id: String) = ServiceCall { request =>
    entityRef(id)
      .ask(UploadNewVersion(request.userId, request.name, request.version, request.tags))
  }

  override def reject(id: String) = ServiceCall { request =>
    entityRef(id)
      .ask(Reject())
  }

  override def submitForReview(id: String) = ServiceCall { request =>
    entityRef(id)
      .ask(SubmitForReview())
  }

  override def approve(id: String) = ServiceCall { request =>
    entityRef(id)
      .ask(Approve())
  }

  override def hello(id: String) = ServiceCall { _ =>
    // Look up the Hello World entity for the given ID.
    entityRef(id)
      .ask(Hello(id))
  }
  /*
      override def useGreeting(id: String) = ServiceCall { request =>
        // Look up the miniapp entity for the given ID.
        val ref = persistentEntityRegistry.refFor[MiniappEntity](id)

        // Tell the entity to use the greeting message specified.
        ref.ask(UseGreetingMessage(request.message))
      }
      */

  /*
  override def greetingsTopic(): Topic[api.GreetingMessageChanged] =
    TopicProducer.singleStreamWithOffset {
      fromOffset =>
        persistentEntityRegistry.eventStream(MiniappEvent.Tag, fromOffset)
          .map(ev => (convertEvent(ev), ev.offset))
    }

  private def convertEvent(helloEvent: EventStreamElement[MiniappEvent]): api.GreetingMessageChanged = {
    helloEvent.event match {
      case GreetingMessageChanged(msg) => api.GreetingMessageChanged(helloEvent.entityId, msg)
    }
  }
  */
}