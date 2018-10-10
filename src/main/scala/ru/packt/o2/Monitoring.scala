package ru.packt.o2

import akka.actor.{ActorRef, ActorSystem, Props, Actor, Terminated}

object Monitoring extends App {
  val system = ActorSystem("monitoring")

  val athena = system.actorOf(Props[Athena], "athena")
  val ares   = system.actorOf(Props(classOf[Ares], athena), "ares")

  athena ! "Hi"
  system.terminate()
}

class Ares(athena: ActorRef) extends Actor {
  override def preStart() = context.watch(athena)
  override def postStop() = println("Ares postStop...")
  def receive = {
    case Terminated => context.stop(self)
  }
}

class Athena extends Actor {
  def receive = {
    case msg =>
      println(s"Athena received $msg")
      context.stop(self)
  }
}
