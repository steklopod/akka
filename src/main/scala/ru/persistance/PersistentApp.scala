package ru.persistance

import akka.actor.{ActorLogging, ActorSystem, Props}
import akka.persistence._
import ru.persistance.Counter._

object PersistentApp extends App {
  val system = ActorSystem("persistent-actors")

  val counter = system.actorOf(Props[Counter])
  counter ! Command(Increment(3))
  counter ! Command(Increment(5))
  counter ! Command(Decrement(3))
  counter ! "print"

  Thread.sleep(1000)

  system.terminate()
}

object Counter {
  sealed trait Operation {
    val count: Int
  }

  case class Increment(override val count: Int) extends Operation
  case class Decrement(override val count: Int) extends Operation

  case class Command(op: Operation)
  case class Event(op: Operation)

  case class State(count: Int)
}

class Counter extends PersistentActor with ActorLogging {
  var state: State           = State(count = 0)
  override def persistenceId = "counter-example"

  println("Starting ........................")

  def updateState(event: Event): Unit = event match {
    case Event(Increment(count)) =>
      state = State(count = state.count + count)
      takeSnapshot
    case Event(Decrement(count)) =>
      state = State(count = state.count - count)
      takeSnapshot
  }

  //В режиме восстановления
  val receiveRecover: Receive = {
    case evt: Event =>
      println(s"Counter receive $evt on recovering mood")
      updateState(evt)
    case SnapshotOffer(_, snapshot: State) =>
      println(s"Counter receive snapshot with data: $snapshot on recovering mood")
      state = snapshot
    case RecoveryCompleted => println(s"Recovery Complete and Now I'll swtich to receiving mode :)")
  }

  // В нормальном режиме
  val receiveCommand: Receive = {
    case cmd @ Command(op) =>
      println(s"Counter receive $cmd")
      persist(Event(op)) { evt => updateState(evt)
      }
    case "print"                               => println(s"The Current state of counter is $state")
    case SaveSnapshotSuccess(metadata)         => println(s"save snapshot succeed.")
    case SaveSnapshotFailure(metadata, reason) => println(s"save snapshot failed and failure is $reason")

  }

  def takeSnapshot = if (state.count % 5 == 0) saveSnapshot(state)

//  override def recovery = Recovery.none
}
