package ru.packt

import akka.actor.{Actor, ActorSystem, Props}

// Define Actor Messages
case class WhoToGreet(who: String)

class Greeter extends Actor {
  def receive = {
    case WhoToGreet(who) => println(s"Hello $who")
  }
}

object HelloAkkaScala extends App {
  val system = ActorSystem("Hello-Akka")

  // Create the 'greeter' actor
  val greeter = system.actorOf(Props[Greeter], "greeter")

  // Send WhoToGreet Message to actor
  greeter ! WhoToGreet("Dima")

}
