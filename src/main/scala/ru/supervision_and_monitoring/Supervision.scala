package ru.supervision_and_monitoring

import akka.actor.SupervisorStrategy._
import akka.actor.{Actor, ActorRef, ActorSystem, OneForOneStrategy, Props}
import ru.supervision_and_monitoring.Aphrodite.{RestartException, ResumeException, StopException}

import scala.concurrent.duration._

object Supervision extends App {
  val system = ActorSystem("supervision")

  val hera = system.actorOf(Props[Hera], "hera")

  hera ! "Stop"
  Thread.sleep(1000)

  system.terminate()
}

class Aphrodite extends Actor {
  override def preStart() = println("Aphrodite preStart hook....")

  override def preRestart(reason: Throwable, message: Option[Any]) = {
    println("Aphrodite preRestart hook...")
    super.preRestart(reason, message)
  }

  override def postRestart(reason: Throwable) = {
    println("Aphrodite postRestart hook...")
    super.postRestart(reason)
  }

  override def postStop() = {
    println("Aphrodite postStop...")
  }

  def receive = {
    case "Resume"  => throw ResumeException
    case "Stop"    => throw StopException
    case "Restart" => throw RestartException
    case _         => throw new Exception
  }
}

object Aphrodite {
  case object ResumeException  extends Exception
  case object StopException    extends Exception
  case object RestartException extends Exception
}

class Hera extends Actor {
  var childRef: ActorRef = _

  override val supervisorStrategy: OneForOneStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 second) {
      case ResumeException  => Resume
      case RestartException => Restart
      case StopException    => Stop // здесь
      case _: Exception     => Escalate
    }

  override def preStart() = {
    childRef = context.actorOf(Props[Aphrodite], "Aphrodite")
//    Thread.sleep(100)
  }

  def receive = {
    case msg =>
      println(s"Hera received $msg")
      childRef ! msg
      Thread.sleep(100)
  }
}