package com.softwaremill.sandbox

import java.util.UUID

import akka.actor.Status.Status
import akka.actor.{ActorSystem, Props, Status}
import akka.cluster.sharding.{ClusterSharding, ClusterShardingSettings}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.ActorMaterializer
import akka.util.Timeout
import ch.megard.akka.http.cors.CorsDirectives
import com.softwaremill.macwire._
import com.typesafe.config.Config
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Random, Success}
import akka.pattern._

trait MainModule extends LazyLogging with CorsDirectives {
  def config: Config
  implicit val system: ActorSystem
  implicit val materializer: ActorMaterializer

  private var serverBinding: Option[ServerBinding] = None
  private val apiConfig = wire[ApiConfig]
  private val userActorMessageExtractor = wire[UserActorMessageExtractor]

//  private lazy val userRegion = system.actorOf(Props(wire[UserActor]))

  private lazy val userRegion =
    ClusterSharding(system).start(
      typeName = "UserActor",
      entityProps = Props[UserActor],
      settings = ClusterShardingSettings(system),
      messageExtractor = userActorMessageExtractor
    )

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

  implicit val timeout = Timeout(5.seconds)

  def routes: Route =
    pathPrefix("user-path") {
      path("create") {

        val r = new Random()
        val pause = r.nextInt(3000)
//        println(s"waiting for $pause")
//        Thread.sleep(pause)
        onSuccess(userRegion ? UserActor.CreateUser(UUID.randomUUID().toString, "andrzej" + pause)) {
          case name: String => complete(name)
        }
      } ~ path("asd") {
        complete("123")
      }
    }
}
