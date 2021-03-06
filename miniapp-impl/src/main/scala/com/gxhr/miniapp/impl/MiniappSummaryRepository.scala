package com.gxhr.miniapp.impl

import akka.Done
import com.gxhr.miniapp.api.MiniappSummary
import slick.jdbc.JdbcBackend.Database
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.ExecutionContext.Implicits.global

class MiniappSummaryRepository(db: Database) {
  class MiniappSummaryTable(tag: Tag) extends Table[MiniappSummary](tag, "miniapp_summary") {
    def id = column[String]("id", O.PrimaryKey)
    def name = column[String]("name")
    def description = column[String]("description")
    def icon = column[String]("icon")
    def * = (id, name, description, icon) <> ((MiniappSummary.apply _).tupled, MiniappSummary.unapply)
  }

  val miniappSummaries = TableQuery[MiniappSummaryTable]

  def selectMiniappSummaries() = {
    db.run(miniappSummaries.result)
  }

  def createTable = miniappSummaries.schema.createIfNotExists

  def addMiniapp(id: String, event: Uploaded): DBIO[Done] = {
    (miniappSummaries += MiniappSummary(id, event.name, "", event.icon)).map(_ => Done)
    /*
    findByIdQuery(id)
      .flatMap {
        case None => miniappSummaries += MiniappSummary(id, event.name, "")
        case _    => DBIO.successful(Done)
      }
      .map(_ => Done)
      .transactionally

     */
  }

  def uploadedNewVersion(id: String, event: UploadedNewVersion): DBIO[Done] = {
    miniappSummaries.insertOrUpdate(MiniappSummary(id, event.name, "", event.icon)).map(_ => Done)
  }

  private def findByIdQuery(id: String): DBIO[Option[MiniappSummary]] = {
    miniappSummaries
      .filter(_.id === id)
      .result
      .headOption
  }
}
