package ru.examples
import akka.actor.{ActorSystem, Props}
import ru.testkit.AkkaSpec

import scala.io.StdIn

class ActorHierarchyExperiments extends AkkaSpec {

  "create top and child actor" in {
    object ActorHierarchyExperiments extends App {
      val system = ActorSystem("testSystem")

      val firstRef = system.actorOf(Props[PrintMyActorRef], "first-actor")
      println(s"First: $firstRef")
      firstRef ! "printit"

      println(">>> Press ENTER to exit <<<")
      try StdIn.readLine()
      finally system.terminate()
    }
  }

  "start and stop actors" in {
    val first = system.actorOf(Props[StartStopActor1], "first")
    first ! "stop"
  }

  "supervise actors" in {
    val supervisingActor = system.actorOf(Props[SupervisingActor], "supervising-actor")
    supervisingActor ! "failChild"
  }
}
