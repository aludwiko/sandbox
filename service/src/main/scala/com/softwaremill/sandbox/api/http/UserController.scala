package com.softwaremill.sandbox.api.http

import java.util.UUID

import akka.http.scaladsl.server.Directives.{complete, onSuccess, pathEnd, pathPrefix, post}
import akka.http.scaladsl.server.Route
import akka.util.Timeout
import com.softwaremill.sandbox.application.UserActor
import com.softwaremill.sandbox.application.UserActor.UserRegion
import kamon.trace.Tracer

import scala.concurrent.duration.DurationInt
import scala.util.Random
import akka.pattern.ask

import scala.concurrent.ExecutionContext

class UserController(userRegion: UserRegion)(implicit executionContext: ExecutionContext) {

  implicit val timeout = Timeout(10.seconds)
  private val random = new Random()

  def routes: Route =
    pathPrefix("user") {
      pathEnd {
        post {
          val traceableUserCreation = Tracer.withNewContext("user-creation") {
            (userRegion ? UserActor.CreateUser(UUID.randomUUID().toString, "andrzej" + random.nextInt(100))).mapTo[String].map { result =>
              Tracer.currentContext.finish()
              result
            }
          }
          onSuccess(traceableUserCreation) {
            case name: String => complete(s"created user: $name")
          }
        }
      }
    }
}
