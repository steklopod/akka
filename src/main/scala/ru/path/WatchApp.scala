package ru.path

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSelection, ActorSystem, Identify, PoisonPill, Props}
import ru.path.Counter.{Dec, Inc}

object WatchApp extends App {
  val system = ActorSystem("Watsh-actor-selection")

  val counter1: ActorRef         = system.actorOf(Props[Counter], "counter")
  val selection1: ActorSelection = system.actorSelection("/user/counter")
  println(s"counter1 REF: $counter1")
  println(s"selection PATH: $selection1")

  counter1 ! Inc(3)
  counter1 ! PoisonPill
  Thread.sleep(1000)

  val counter2: ActorRef         = system.actorOf(Props[Counter], "counter")
  val selection2: ActorSelection = system.actorSelection("/user/counter")
  println(s"counter2 REF: $counter2")
  println(s"selection PATH: $selection2")

  val watcher = system.actorOf(Props[Watcher], "watcher")

  system.terminate()
}

class Counter extends Actor {
  var count = 0

  def receive = {
    case Inc(x) => count += x
    case Dec(x) => count -= x
  }
}

object Counter {
  final case class Inc(num: Int)
  final case class Dec(num: Int)
}

class Watcher extends Actor {
  var counterRef: ActorRef = _

  val selection: ActorSelection = context.actorSelection("/user/counter")
  selection ! Identify(None)

  def receive = {
    case ActorIdentity(_, Some(ref)) => println(s"counter1 REF wathcer: $ref")
    case ActorIdentity(_, None)      => println("Actor selection for actor doesn't live :( ")
  }
}
