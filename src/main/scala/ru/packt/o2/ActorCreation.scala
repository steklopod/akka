package ru.packt.o2

import akka.actor.{Actor, ActorSystem, Props}

object Creation extends App {
  val system = ActorSystem("creation")

  val zeus = system.actorOf(Props[Zeus], "zeus")

  zeus ! Zeus.StartMusic
  zeus ! Zeus.StopMusic

  system.terminate()
}


object Apollo {
  case object Play
  case object Stop

  def props = Props[Apollo]
}

class Apollo extends Actor {
  import Apollo._

  def receive = {
    case Play => println("Music Started .............")
    case Stop => println("Music Stopped .............")
  }
}

object Zeus {
  case object StartMusic
  case object StopMusic
}

class Zeus extends Actor {
  import Zeus._

  def receive = {
    case StartMusic =>
      val apollo = context.actorOf(Apollo.props)
      apollo ! Apollo.Play
    case StopMusic => println("I don't want to stop music.")
  }
}