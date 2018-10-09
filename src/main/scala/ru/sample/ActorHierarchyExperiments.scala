package ru.sample

import akka.actor.{ActorRef, ActorSystem, Props}
import org.slf4j.{Logger, LoggerFactory}

import scala.io.StdIn

// runMain ru.sample.ActorHierarchyExperiments
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
