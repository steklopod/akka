package ru.clustering.load_balancing

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import ru.clustering.commons._

class Backend extends Actor { 

  def receive = {
    case Add(num1, num2) =>
      println(s"I'm a backend with path: ${self} and I received add operation.")

  }

}

object Backend {
  def initiate(port: Int){
     val config = ConfigFactory.parseString(s"akka.remote.netty.tcp.port=$port").
      withFallback(ConfigFactory.parseString("akka.cluster.roles = [backend]")).
      withFallback(ConfigFactory.load("loadbalancer"))

    val system = ActorSystem("ClusterSystem", config)

    val Backend = system.actorOf(Props[Backend], name = "backend")
  }
}