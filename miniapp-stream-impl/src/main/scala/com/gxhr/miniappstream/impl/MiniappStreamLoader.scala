package com.gxhr.miniappstream.impl

import com.lightbend.lagom.scaladsl.api.ServiceLocator.NoServiceLocator
import com.lightbend.lagom.scaladsl.server._
import com.lightbend.lagom.scaladsl.devmode.LagomDevModeComponents
import play.api.libs.ws.ahc.AhcWSComponents
import com.gxhr.miniappstream.api.MiniappStreamService
import com.gxhr.miniapp.api.MiniappService
import com.softwaremill.macwire._

class MiniappStreamLoader extends LagomApplicationLoader {

  override def load(context: LagomApplicationContext): LagomApplication =
    new MiniappStreamApplication(context) {
      override def serviceLocator: NoServiceLocator.type = NoServiceLocator
    }

  override def loadDevMode(context: LagomApplicationContext): LagomApplication =
    new MiniappStreamApplication(context) with LagomDevModeComponents

  override def describeService = Some(readDescriptor[MiniappStreamService])
}

abstract class MiniappStreamApplication(context: LagomApplicationContext)
  extends LagomApplication(context)
    with AhcWSComponents {

  // Bind the service that this server provides
  override lazy val lagomServer: LagomServer = serverFor[MiniappStreamService](wire[MiniappStreamServiceImpl])

  // Bind the MiniappService client
  lazy val miniappService: MiniappService = serviceClient.implement[MiniappService]
}
