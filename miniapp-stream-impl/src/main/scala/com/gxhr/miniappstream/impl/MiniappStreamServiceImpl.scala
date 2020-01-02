package com.gxhr.miniappstream.impl

import akka.{Done, NotUsed}
import com.lightbend.lagom.scaladsl.api.ServiceCall
import com.gxhr.miniappstream.api.MiniappStreamService
import com.gxhr.miniapp.api.MiniappService

import scala.concurrent.Future
import scala.io.Source

/**
  * Implementation of the MiniappStreamService.
  */
class MiniappStreamServiceImpl(miniappService: MiniappService) extends MiniappStreamService {
  def stream = ServiceCall { hellos =>
    Future.successful(hellos.mapAsync(8)(miniappService.hello(_).invoke()))
    //Future.successful(Source[NotUsed, Done])
  }
}
