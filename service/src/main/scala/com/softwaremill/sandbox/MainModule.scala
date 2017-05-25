package com.softwaremill.sandbox

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import ch.megard.akka.http.cors.CorsDirectives
import com.softwaremill.macwire._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Random, Success}

trait MainModule extends LazyLogging with CorsDirectives {
  def config: Config
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  private var serverBinding: Option[ServerBinding] = None
  private val apiConfig = wire[ApiConfig]

  def startApi(): Future[ServerBinding] = {
    logger.info(s"Starting API at: ${apiConfig.host}:${apiConfig.port}")
    Http().bindAndHandle(routes, apiConfig.host, apiConfig.port).andThen {
      case Success(binding) =>
        serverBinding = Some(binding)
        logger.info(s"API ready at ${apiConfig.host}:${apiConfig.port}")
      case Failure(e) => logger.error("Unable to start API", e)
    }
  }

  def stopApi(): Unit = serverBinding.foreach(binding => Await.ready(binding.unbind(), 1.minute))

  def routes: Route =
    pathPrefix("user") {
      val r = new Random()
      Thread.sleep(r.nextInt(1000)`)
      complete("asd")
    }

}
