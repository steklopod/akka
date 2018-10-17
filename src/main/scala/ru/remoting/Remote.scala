package ru.remoting

import akka.actor.{Actor, ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import Worker._

object MembersService extends App {
  val config = ConfigFactory.load.getConfig("MembersService")

  val system = ActorSystem("MembersService", config)

  val worker = system.actorOf(Props[Worker], "remote-worker")

  println(s"Worker actor path is ${worker.path}")
}

object MemberServiceLookup extends App {
  val config = ConfigFactory.load.getConfig("MemberServiceLookup")

  val system = ActorSystem("MemberServiceLookup", config)

  val worker = system.actorSelection("akka.tcp://MembersService@127.0.0.1:2552/user/remote-worker")

  worker ! Worker.Work("Hi Remote Actor")
}

object MembersServiceRemoteCreation extends App {
  val config = ConfigFactory.load.getConfig("MembersServiceRemoteCreation")

  val system = ActorSystem("MembersServiceRemoteCreation", config)

  val workerActor = system.actorOf(Props[Worker], "workerActorRemote")

  println(s"The remote path of worker Actor is ${workerActor.path}")

  workerActor ! Worker.Work("Hi Remote Worker")
}


class Worker extends Actor {

  def receive = {
    case msg: Work =>
      println(s"I received Work Message and My ActorRef: $self")
  }
}

object Worker {
  case class Work(message: String)
}