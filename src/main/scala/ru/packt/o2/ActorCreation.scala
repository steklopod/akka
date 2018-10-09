package ru.packt.o2

import akka.actor.{Actor, ActorSystem, Props}

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

object Creation extends App {

  // Create the 'creation' actor system
  val system = ActorSystem("creation")

  // Create the 'Zeus' actor
  val zeus = system.actorOf(Props[Zeus], "zeus")

  //send StartMusic Message to actor
  zeus ! Zeus.StartMusic

  // Send StopMusic Message to actor
  zeus ! Zeus.StopMusic

  //shutdown system
  system.terminate()

}
