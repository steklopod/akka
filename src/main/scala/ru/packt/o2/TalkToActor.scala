package ru.packt.o2

import akka.actor.{ActorRef, ActorSystem, Props, Actor}
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

case class User(username: String, email: String)

object Recorder {
  case class NewUser(user: User)

  def props(checker: ActorRef, storage: ActorRef) =
    Props(new Recorder(checker, storage))
}

object Checker {
  case class CheckUser(user: User)

  case class WhiteUser(user: User)

  case class BlackUser(user: User)
}

object Storage {
  case class AddUser(user: User)
}

class Storage extends Actor {
  import Storage._

  var users = List.empty[User]

  def receive = {
    case AddUser(user) =>
      println(s"Storage: ${user} stored")
      users = user :: users
  }
}

class Checker extends Actor {
  import Checker._

  val blackList = List(
    User("Adam", "adam@mail.com")
  )

  def receive = {
    case CheckUser(user) =>
      if (blackList.contains(user)) {
        println(s"Checker: ${user} is in blackList.")
        sender ! BlackUser(user)
      } else {
        println(s"Checker: ${user} isn't in blackList.")
        sender ! WhiteUser(user)
      }
  }
}

class Recorder(checker: ActorRef, storage: ActorRef) extends Actor {
  import Recorder._
  import scala.concurrent.ExecutionContext.Implicits.global

  implicit val timeout = Timeout(5 seconds)

  def receive = {
    case NewUser(user) =>
      println(s"Recorder receives NewUser for ${user}")
      checker ? Checker.CheckUser(user) map {
        case Checker.WhiteUser(user) =>
          storage ! Storage.AddUser(user)
        case Checker.BlackUser(user) =>
          println(s"Recorder: ${user} in in black user.")
      }
  }
}

object TalkToActor extends App {

  // Create the 'talk-to-actor' actor system
  val system = ActorSystem("talk-to-actor")

  // Create the 'checker' actor
  val checker = system.actorOf(Props[Checker], "checker")

  // Create the 'storage' actor
  val storage = system.actorOf(Props[Storage], "storage")

  // Create the 'recorder' actor
  val recorder = system.actorOf(Recorder.props(checker, storage), "recorder")

  //send NewUser Message to Recorder
  recorder ! Recorder.NewUser(User("Jon", "jon@packt.com"))

  //shutdown system
  system.terminate()

}
