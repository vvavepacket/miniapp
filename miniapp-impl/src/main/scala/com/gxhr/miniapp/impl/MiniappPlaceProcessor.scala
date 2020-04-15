package com.gxhr.miniapp.impl

import com.lightbend.lagom.scaladsl.persistence.{AggregateEventTag, ReadSideProcessor}
import com.lightbend.lagom.scaladsl.persistence.slick.SlickReadSide

class MiniappPlaceProcessor(
                             readSide: SlickReadSide,
                             miniappPlaceRepo: MiniappPlaceRepository
                           ) extends ReadSideProcessor[MiniappEvent] {
  override def buildHandler(): ReadSideProcessor.ReadSideHandler[MiniappEvent] = {
    readSide
      .builder[MiniappEvent]("miniapp-place")
      .setGlobalPrepare(miniappPlaceRepo.createTable)
      .setEventHandler[AddedPlaceToMiniapp] { envelope =>
        miniappPlaceRepo.addMiniappPlace(envelope.event)
      }
      .build()
  }

  override def aggregateTags: Set[AggregateEventTag[MiniappEvent]] = MiniappEvent.Tag.allTags
}
