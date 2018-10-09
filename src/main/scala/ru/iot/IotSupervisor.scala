package ru.iot

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import scala.io.StdIn

object IotApp extends App {
  val system = ActorSystem("iot-system")
  try {
    val supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor") // Create top level supervisor
    StdIn.readLine()
  } finally system.terminate()
}

object IotSupervisor { def props(): Props = Props(new IotSupervisor) }

class IotSupervisor extends Actor with ActorLogging {
  override def preStart(): Unit = log.info("IoT Application started")
  override def postStop(): Unit = log.info("IoT Application stopped")
  override def receive: Receive = Actor.emptyBehavior // Нет необходимости обрабатывать сообщения
}
