package ru.hello

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import org.slf4j.{Logger, LoggerFactory}

import scala.io.StdIn

// runMain ru.hello.ActorHierarchyExperiments
object ActorHierarchyExperiments extends App {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("testSystem")

  val firstRef: ActorRef = system.actorOf(Props[PrintMyActorRef], "first-actor")
  val first              = system.actorOf(Props[StartStopActor1], "first")
  val supervisingActor   = system.actorOf(Props[SupervisingActor], "supervising-actor")

  log.info(s"firstFer: $firstRef")
  log.info(s"first: $first")
  log.info(s"supervisingActor: $supervisingActor")

  //  firstRef ! "printit"

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine()
  finally first ! "stop"; system.terminate()
  //  first ! "stop"
  //  supervisingActor ! "failChild"
}


class PrintMyActorRef extends Actor {
  override def receive: Receive = {
    case "printit" ⇒
      val secondRef = context.actorOf(Props.empty, "second-actor")
      println(s"Second: $secondRef")
  }
}

class StartStopActor1 extends Actor {
  override def preStart(): Unit = {
    context.actorOf(Props[StartStopActor2], "second")
    println("first started")
  }
  override def postStop(): Unit = println("first stopped")
  override def receive: Receive = { case "stop" ⇒ context.stop(self) }
}

class StartStopActor2 extends Actor {
  override def preStart(): Unit = println("second started")
  override def postStop(): Unit = println("second stopped")
  override def receive: Receive = Actor.emptyBehavior //we don't want to handle any messages in the actor.
}

class SupervisingActor extends Actor {
  val child: ActorRef           = context.actorOf(Props[SupervisedActor], "supervised-actor")
  override def receive: Receive = { case "failChild" ⇒ child ! "fail" }
}

class SupervisedActor extends Actor {
  override def preStart(): Unit = println("supervised actor started")
  override def postStop(): Unit = println("supervised actor stopped")
  override def receive: Receive = { case "fail" ⇒ throw new Exception("I failed!") }
}