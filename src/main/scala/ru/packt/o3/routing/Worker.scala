package ru.packt.o3.routing

import akka.actor.Actor
import ru.packt.o3.routing.Worker.Work

class Worker extends Actor {

  def receive = {
    case msg: Work => println(s"I received Work Message and My ActorRef: $self")
  }
}

object Worker {
  case class Work()
}
