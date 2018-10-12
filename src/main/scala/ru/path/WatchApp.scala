package ru.path

import akka.actor.{Actor, ActorIdentity, ActorRef, ActorSelection, ActorSystem, Identify, Props}
import ru.path.Counter.{Dec, Inc}

object WatchApp extends App {
  val system = ActorSystem("Watsh-actor-selection")

  val counter = system.actorOf(Props[Counter], "counter")
  val watcher = system.actorOf(Props[Watcher], "watcher")
  Thread.sleep(1000)
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
    case ActorIdentity(_, Some(ref)) => println(s"Actor Reference for counter is $ref")
    case ActorIdentity(_, None)      => println("Actor selection for actor doesn't live :( ")
  }
}
