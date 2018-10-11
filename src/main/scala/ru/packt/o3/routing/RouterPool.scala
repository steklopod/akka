package ru.packt.o3.routing

import akka.actor.{Actor, ActorRef, Props}
import ru.packt.o3.routing.Worker._

class RouterPool extends Actor {

  var routees: List[ActorRef] = _

  override def preStart() = {
    routees = List.fill(5)( context.actorOf(Props[Worker]) )
  }

  def receive() = {
    case msg: Work =>
      println("I'm A Router and I received a Message.....")
      routees(util.Random.nextInt(routees.size)) forward msg
  }
}

class RouterGroup(routees: List[String]) extends Actor {

  def receive = {
    case msg: Work =>
      println(s"I'm a Router Group and I receive Work Message....")
      context.actorSelection(routees(util.Random.nextInt(routees.size))) forward msg
  }
}
