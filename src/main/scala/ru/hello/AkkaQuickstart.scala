package ru.hello

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import ru.hello.Greeter.{Greet, WhoToGreet}
import ru.hello.Printer.Greeting

object AkkaQuickstart extends App {
  import Greeter._

  val system: ActorSystem = ActorSystem("helloAkka")

  val printer: ActorRef        = system.actorOf(Printer.props, "printerActor")
  val howdyGreeter: ActorRef   = system.actorOf(Greeter.props("Howdy", printer), "howdyGreeter")
  val helloGreeter: ActorRef   = system.actorOf(Greeter.props("Hello", printer), "helloGreeter")
  val goodDayGreeter: ActorRef = system.actorOf(Greeter.props("Good day", printer), "goodDayGreeter")

  //#main-send-messages
  howdyGreeter ! WhoToGreet("Akka")
  howdyGreeter ! Greet

  howdyGreeter ! WhoToGreet("Dima")
  howdyGreeter ! Greet

  helloGreeter ! WhoToGreet("Scala")
  helloGreeter ! Greet

  goodDayGreeter ! WhoToGreet("Play")
  goodDayGreeter ! Greet
}

object Greeter {
  def props(message: String, printerActor: ActorRef): Props = Props(new Greeter(message, printerActor))
  final case class WhoToGreet(who: String)
  case object Greet
}

class Greeter(message: String, printerActor: ActorRef) extends Actor {
  var greeting = ""

  def receive: PartialFunction[Any, Unit] = {
    case WhoToGreet(who) => greeting = message + ", " + who
    case Greet           => printerActor ! Greeting(greeting)
  }
}

object Printer {
  def props: Props = Props[Printer]
  final case class Greeting(greeting: String)
}

class Printer extends Actor with ActorLogging {
  def receive: PartialFunction[Any, Unit] = {
    case Greeting(greeting) => log.info("Greeting received (from " + sender() + "): " + greeting)
  }
}
