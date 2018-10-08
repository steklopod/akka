package ru.example

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.slf4j.{Logger, LoggerFactory}
import scala.io.StdIn

//sbt "runMain ru.example.ActorHierarchyExperiments"
object ActorHierarchyExperiments extends App {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("testSystem")

  val firstRef = system.actorOf(Props[PrintMyActorRef], "first-actor")
  println(s"First: $firstRef")
  firstRef ! "printit"

  val first            = system.actorOf(Props[StartStopActor1], "first")
  val supervisingActor = system.actorOf(Props[SupervisingActor], "supervising-actor")

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine()
  finally first ! "stop"

  //  first ! "stop"
//  supervisingActor ! "failChild"
}

class PrintMyActorRef extends Actor {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  override def receive: Receive = {
    case "printit" ⇒
      val secondRef: ActorRef = context.actorOf(Props.empty, "second-actor")
      println(s"Second: $secondRef")
  }
}

class StartStopActor1 extends Actor {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  override def preStart(): Unit = {
    log.info("first started")
    context.actorOf(Props[StartStopActor2], "second")
  }
  override def postStop(): Unit = println("first stopped")

  override def receive: Receive = {
    case "stop" ⇒ context.stop(self)
  }
}

class StartStopActor2 extends Actor {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  override def preStart(): Unit = log.info("second started")
  override def postStop(): Unit = log.info("second stopped")

  // Actor.emptyBehavior is a useful placeholder when we don't
  // want to handle any messages in the actor.
  override def receive: Receive = Actor.emptyBehavior
}

class SupervisingActor extends Actor {
  val child: ActorRef =
    context.actorOf(Props[SupervisedActor], "supervised-actor")

  override def receive: Receive = { case "failChild" ⇒ child ! "fail" }
}

class SupervisedActor extends Actor {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  override def preStart(): Unit = log.info("supervised actor started")
  override def postStop(): Unit = log.info("supervised actor stopped")

  override def receive: Receive = {
    case "fail" ⇒
      println("supervised actor fails now")
      throw new Exception("I failed!")
  }
}
