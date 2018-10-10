package com.packt.akka

import akka.actor.{ActorSystem, Props}
import ru.packt.o4_persistence.persistent_fsm_and_query.Account

object PersistentFSM extends App {
  import ru.packt.o4_persistence.persistent_fsm_and_query.Account._

  val system = ActorSystem("persistent-fsm-actors")

  val account = system.actorOf(Props[Account])

  account ! Operation(1000, CR)

  account ! Operation(10, DR)

  Thread.sleep(1000)

  system.terminate()

}
