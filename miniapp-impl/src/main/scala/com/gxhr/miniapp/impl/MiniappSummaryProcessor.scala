package com.gxhr.miniapp.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide

class MiniappSummaryProcessor(
                               readSide: SlickReadSide,
                               miniappSummaryRepo: MiniappSummaryRepository
                             ) extends ReadSideProcessor[MiniappEvent] {
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[MiniappEvent] = {
    readSide
      .builder[MiniappEvent]("miniapp-summary")
      .setGlobalPrepare(miniappSummaryRepo.createTable)
      .setEventHandler[Uploaded] { envelope =>
        miniappSummaryRepo.addMiniapp(envelope.entityId, envelope.event)
      }
      .setEventHandler[UploadedNewVersion] { envelope =>
        miniappSummaryRepo.uploadedNewVersion(envelope.entityId, envelope.event)

      }
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[MiniappEvent]] = MiniappEvent.Tag.allTags
}
