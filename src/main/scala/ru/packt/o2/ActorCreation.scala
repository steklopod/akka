package ru.packt.o2

import akka.actor.{Actor, ActorSystem, Props}
import ru.packt.o2.Apollo.{Play, Stop}
import ru.packt.o2.Zeus.{StartMusic, StopMusic}
import scala.io.StdIn

object ActorCreation extends App {
  val system = ActorSystem("creation")

  val zeus   = system.actorOf(Props[Zeus], "zeus")
  val apollo = system.actorOf(Apollo.props)

  zeus ! Zeus.StartMusic
  zeus ! Zeus.StopMusic

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine
  finally apollo ! Apollo.Stop; system.terminate
}

object Zeus {
  case object StartMusic
  case object StopMusic
}

class Zeus extends Actor {
  def receive = {
    case StartMusic =>
      val apollo = context.actorOf(Apollo.props)
      apollo ! Apollo.Play
    case StopMusic => println("I don't want to stop music.")
  }
}

object Apollo {
  sealed trait PlayMsg
  case object Play extends PlayMsg
  case object Stop extends PlayMsg

  def props = Props[Apollo]
}

class Apollo extends Actor {
  override def postStop() = println("I don't want to die")
  def receive = {
    case Play => println("Apollo music Started .............")
    case Stop => println("Apollo music Stopped .............")
  }
}