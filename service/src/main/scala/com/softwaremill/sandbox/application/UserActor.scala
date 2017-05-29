package com.softwaremill.sandbox.application

import akka.actor.{ActorLogging, ActorRef, Status}
import akka.cluster.sharding.ShardRegion.HashCodeMessageExtractor
import akka.persistence.PersistentActor
import com.softwaremill.sandbox.application.UserActor.{CreateUser, UserCreated}
import com.softwaremill.tagging.@@

case class UserState(name: String)

class UserActor extends PersistentActor with ActorLogging {

  var userState: Option[UserState] = None

  override def receiveRecover: Receive = {
    case event: UserCreated => userState = Some(UserState(event.name))
  }
  override def receiveCommand: Receive = {
    case command: CreateUser =>
      val currentSender = sender()
      log.debug("creating used")
      Thread.sleep(2500)
      persist(UserCreated(command.name)) { e =>
        userState = Some(UserState(e.name))
        Thread.sleep(1500)
        log.debug("user created")
        currentSender ! Status.Success(e.name)
      }
  }
  override def persistenceId: String = s"UA-${self.path.name}"
}

trait UserCommand {
  def userId: String
}

object UserActor {
  case class CreateUser(userId: String, name: String) extends UserCommand
  case class UserCreated(name: String)

  trait UserRegionTag

  type UserRegion = ActorRef @@ UserRegionTag
}

class UserActorMessageExtractor extends HashCodeMessageExtractor(10) {

  override def entityId(message: Any): String = message match {
    case command: UserCommand => command.userId
  }
}
