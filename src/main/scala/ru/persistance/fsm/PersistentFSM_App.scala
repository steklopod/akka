package ru.persistance.fsm
import akka.actor.{ActorSystem, Props}
import ru.persistance.fsm.Account._

object PersistentFSM_App extends App {
  val system = ActorSystem("persistent-fsm-actors")
  val account = system.actorOf(Props[Account])

  account ! Operation(1000, CR)
  account ! Operation(10, DR)
  Thread.sleep(1000)
  system.terminate()
}
