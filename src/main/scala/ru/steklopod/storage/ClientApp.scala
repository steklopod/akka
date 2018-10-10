package ru.steklopod.storage

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

import scala.io.StdIn

object ClientApp extends App {
  import com.typesafe.config.ConfigFactory
  import scala.concurrent.duration._

  // переопределим часть конфигурации секцией "client"
  val rootConfig = ConfigFactory.load()
  val config     = rootConfig.getConfig("client").withFallback(rootConfig)

  // создадим актор систему и актора-клиента
  val actorSystem      = ActorSystem("client-system", config)
  val client: ActorRef = actorSystem.actorOf(Props[Client])

  // полный akka-путь к Storage
  val storagePath = "akka.tcp://storage-system@127.0.0.1:2552/user/storage"

  val storageSelection = actorSystem.actorSelection(storagePath)

  // ждем ответа
  val resolveTimeout = FiniteDuration(10, SECONDS)

  storageSelection
    .resolveOne(resolveTimeout)
    .foreach { storage: ActorRef =>
      // командуем клиенту присоединиться к хранилищу
      println(s"Connected to $storage")
      client ! Client.Connect(storage)
    }(actorSystem.dispatcher) // контекст в которым выполнится Future
}

class Client extends Actor {

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

object Client {
  final case class Connect(storage: ActorRef)
  case object Process
}
