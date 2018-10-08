package ru.sample

import akka.actor.{ActorSystem, Props}
import org.slf4j.{Logger, LoggerFactory}

import scala.io.StdIn

//sbt "runMain ru.example.ActorHierarchyExperiments2"
object ActorHierarchyExperiments extends App {
  val log: Logger = LoggerFactory.getLogger(this.getClass)

  val system = ActorSystem("testSystem")

//  val firstRef = system.actorOf(Props[PrintMyActorRef2], "first-actor")
//  println(s"First: $firstRef")
//  firstRef ! "printit"

  val first            = system.actorOf(Props[StartStopActor1], "first")
  val supervisingActor = system.actorOf(Props[SupervisingActor], "supervising-actor")

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine()
  finally first ! "stop"

  //  first ! "stop"
  supervisingActor ! "failChild"
}