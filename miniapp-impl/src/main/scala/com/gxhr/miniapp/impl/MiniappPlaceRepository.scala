package com.gxhr.miniapp.impl

import akka.Done
import com.gxhr.miniapp.api.{MiniappPlace, MiniappSummary}
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

class MiniappPlaceRepository(db: Database) {
  class MiniappPlaceTable(tag: Tag) extends Table[MiniappPlace](tag, "miniapp_place") {
    def miniappId = column[String]("miniappId")
    def placeId = column[String]("placeId")
    def pk = primaryKey("pk_a", (miniappId, placeId))
    def * = (miniappId, placeId) <> ((MiniappPlace.apply _).tupled, MiniappPlace.unapply)
  }

  val miniappPlaces = TableQuery[MiniappPlaceTable]

  def selectMiniappPlaces() = {
    db.run(miniappPlaces.result)
  }

  // manually create table because there is bug in github
  // https://github.com/slick/slick/issues/1999
  def createTable = miniappPlaces.schema.create


  def addMiniappPlace(event: AddedPlaceToMiniapp): DBIO[Done] = {
    miniappPlaces.filter(i => i.miniappId === event.miniappId && i.placeId === event.placeId).exists.result
      .map {
        case true => {
          println("true")
          Done
        }
        case _ => {
          println("false")
          db.run(miniappPlaces.forceInsert(MiniappPlace(event.miniappId, event.placeId)))
          Done
        }
      }
  }
}
