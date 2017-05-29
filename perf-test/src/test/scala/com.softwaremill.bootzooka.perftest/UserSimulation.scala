package com.softwaremill.bootzooka.perftest

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import io.gatling.http.protocol.HttpProtocolBuilder

import scala.util.Random
import scala.concurrent.duration._

class UserSimulation extends Simulation {

  val baseURL = "http://localhost:9000"

//  val httpConf: HttpProtocolBuilder = http.acceptHeader("application/json, text/html, text/plain, */*").acceptEncodingHeader("gzip, deflate")
  val httpConf = http.baseURL(baseURL).contentTypeHeader(HttpHeaderValues.ApplicationJson)

  def feeder = Iterator.continually(Map("login" -> randomLogin))

  val userScenario = scenario("User scenario")
    .exec(
      http("create user")
      .post("/user"))

  def randomLogin = Random.alphanumeric.take(10 + Random.nextInt(10)).mkString


  setUp(
    userScenario
      .inject(rampUsers(100) over(1 minute) )
      .protocols(httpConf))
    .assertions(forAll.failedRequests.percent.is(0))

}



