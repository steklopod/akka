package ru.packt.o2

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import ru.packt.o2.Checker.{BlackUser, CheckUser, WhiteUser}
import ru.packt.o2.Recorder.NewUser
import ru.packt.o2.Storage.AddUser

import scala.concurrent.duration._

object TalkToActor extends App {
    val system = ActorSystem("talk-to-actor")

    val checker  = system.actorOf(Props[Checker], "checker")
    val storage  = system.actorOf(Props[Storage], "storage")
    val recorder = system.actorOf(Recorder.props(checker, storage), "recorder")

    recorder ! Recorder.NewUser(User("Jon", "jon@packt.com"))

    system.terminate()
  }


case class User(username: String, email: String)


object Storage {
  case class AddUser(user: User)
}


object Checker {
  sealed trait CheckerMsg
  case class CheckUser(user: User) extends CheckerMsg

  sealed trait CheckerResponse
  case class BlackUser(user: User) extends CheckerResponse
  case class WhiteUser(user: User) extends CheckerResponse
}


class Storage extends Actor {
  var users = List.empty[User]

  def receive = {
    case AddUser(user) =>
      println(s"Storage: $user stored")
      users = user :: users
  }
}


class Checker extends Actor {
  val blackList = List(User("Adam", "adam@mail.com"))

  def receive = {
    case CheckUser(user) =>
      if (blackList.contains(user)) {
        println(s"BAD. Checker: $user находится в черном списке.")
        sender ! BlackUser(user)
      } else {
        println(s"OK. Checker: $user отсутствует в черном списке.")
        sender ! WhiteUser(user)
      }
  }
}


object Recorder {
    case class NewUser(user: User)

    def props(checker: ActorRef, storage: ActorRef) = Props(new Recorder(checker, storage))
  }


class Recorder(checker: ActorRef, storage: ActorRef) extends Actor {
  import scala.concurrent.ExecutionContext.Implicits.global
  implicit val timeout = Timeout(5 seconds)

  def receive = {
    case NewUser(user) =>
      println(s"Регистратор проверяет нового пользователя $user")
      checker ? Checker.CheckUser(user) map {
        case Checker.WhiteUser(user) => storage ! Storage.AddUser(user)
        case Checker.BlackUser(user) => println(s"Recorder: $user в черном списке.")
      }
  }
}