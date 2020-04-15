package com.gxhr.miniapp.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator
import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.persistence.cassandra.{CassandraPersistenceComponents, WriteSideCassandraPersistenceComponents}
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.gxhr.miniapp.api.{MiniappService, UploadMessageDone}
import com.lightbend.lagom.scaladsl.broker.kafka.LagomKafkaComponents
import com.lightbend.lagom.scaladsl.playjson.{JsonSerializer, JsonSerializerRegistry}
import com.softwaremill.macwire._
import com.lightbend.lagom.scaladsl.akka.discovery.AkkaDiscoveryComponents
import com.lightbend.lagom.scaladsl.client.ConfigurationServiceLocatorComponents
import com.lightbend.lagom.scaladsl.persistence.jdbc.{JdbcPersistenceComponents, ReadSideJdbcPersistenceComponents}
import com.lightbend.lagom.scaladsl.persistence.slick.{ReadSideSlickPersistenceComponents, SlickPersistenceComponents}
import play.api.db.HikariCPComponents

class MiniappLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new MiniappApplication(context)
      with ConfigurationServiceLocatorComponents

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new MiniappApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[MiniappService])
}

abstract class MiniappApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    //with JdbcPersistenceComponents
    //with ReadSideJdbcPersistenceComponents
    with ReadSideSlickPersistenceComponents
    with WriteSideCassandraPersistenceComponents
    //with SlickPersistenceComponents
    //with CassandraPersistenceComponents
    //with WriteSideCassandraPersistenceComponents
    //with SlickPersistenceComponents
    with LagomKafkaComponents
    with HikariCPComponents
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer =
    serverFor[MiniappService](wire[MiniappServiceImpl])
      .additionalRouter(wire[FileUploadRouter].router)

  // Register the JSON serializer registry
  override lazy val jsonSerializerRegistry: JsonSerializerRegistry = MiniappSerializerRegistry

  // Register the miniapp persistent entity
  persistentEntityRegistry.register(wire[MiniappEntity])

  lazy val miniappSummaryRepo: MiniappSummaryRepository =
    wire[MiniappSummaryRepository]
  readSide.register(wire[MiniappSummaryProcessor])

  lazy val miniappPlaceRepo: MiniappPlaceRepository =
    wire[MiniappPlaceRepository]
  readSide.register(wire[MiniappPlaceProcessor])


}
