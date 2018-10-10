package ru.steklopod

import akka.actor.{Actor, ActorRef}

import scala.io.StdIn

object Client {
  final case class Connect(storage: ActorRef)
  case object Process
}

class Client extends Actor {

  // обработчик сообщений начального состояния
  override def receive: Actor.Receive = {
    // в начальном состоянии дожидаемся команды присоединиться хранилищу
    case Client.Connect(storage) =>
      // переходим в рабочее состояние
      context become process(storage)
      // посылаем себе сообщение для начала работы в новом состоянии
      self ! Client.Process
  }

  // обработчик сообщений рабочего состояния
  def process(storage: ActorRef): Receive = {
    // считывание команд с клавиатуры
    case Client.Process =>
      println("Enter command:")

      // передача соответствующих команд хранилищу
      StdIn.readLine().split(' ') match {
        case Array("get", key)        => storage ! Storage.Get(key)
        case Array("put", key, value) => storage ! Storage.Put(key, value)
        case Array("delete", key)     => storage ! Storage.Delete(key)
        case Array("stop")            => context.system.terminate()
        case _                        => println("Unknown command")
      }

      Thread.sleep(100) // дадим время обработать сообщение
      self ! Client.Process // "рекурсия"

    // прием ответов от хранилища
    case Storage.GetResult(key, value) => println(s"Received: $key -> $value")

    case Storage.Ack => println("Received ack.")
  }
}
