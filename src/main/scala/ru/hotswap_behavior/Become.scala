package ru.hotswap_behavior

import akka.actor.{Actor, ActorSystem, Props, Stash}
import akka.event.Logging
import ru.hotswap_behavior.UserStorage._

object BecomeHotswap extends App {
  import UserStorage._

  val system = ActorSystem("Hotswap-Become")

  val userStorage = system.actorOf(Props[UserStorage], "userStorage")

  userStorage ! Operation(DBOperation.Create, Some(User("Admin", "admin@packt.com")))
  userStorage ! Connect

  userStorage ! Disconnect

  Thread.sleep(100)

  system.terminate()
}

case class User(username: String, email: String)

object UserStorage {

  trait DBOperation
  object DBOperation {
    case object Create extends DBOperation
    case object Update extends DBOperation
    case object Read   extends DBOperation
    case object Delete extends DBOperation
  }

  case object Connect
  case object Disconnect
  case class Operation(dBOperation: DBOperation, user: Option[User])
}

class UserStorage extends Actor with Stash {

  def receive = disconnected

  def connected: Actor.Receive = {
    case Disconnect =>
      println("User Storage Disconnect from DB")
      context.unbecome()
    case Operation(op, user) =>
      println(s"User Storage receive ${op} to do in user: ${user}")
  }

  def disconnected: Actor.Receive = {
    case Connect =>
      println(s"User Storage connected to DB")
      unstashAll()
      context.become(connected)
    case _ =>
      stash()
  }
}



///////////////////EXAMPLE #2

case object Swap

class Swapper extends Actor {
  import context._
  val log = Logging(system, this)

  def receive = {
    case Swap ⇒
      log.info("Hi")
      become({
        case Swap ⇒
          log.info("Ho")
          unbecome() // сбрасываем последний 'become' (для веселья)
      }, discardOld = false) // push on top instead of replace
  }
}

object SwapperApp extends App {
  val system = ActorSystem("SwapperSystem")
  val swap   = system.actorOf(Props[Swapper], name = "swapper")
  swap ! Swap // logs Hi
  swap ! Swap // logs Ho
  swap ! Swap // logs Hi
  swap ! Swap // logs Ho
  swap ! Swap // logs Hi
  swap ! Swap // logs Ho
}
