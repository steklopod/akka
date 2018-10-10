package ru.steklopod

import akka.actor._

import scala.io.StdIn

object StorageApp extends App {
  val actorSystem = ActorSystem("storage-system")
  val storage: ActorRef = actorSystem.actorOf(Props[Storage], "storage")
  StdIn.readLine()
  actorSystem.terminate()
}

object ClientApp extends App {
  import com.typesafe.config.ConfigFactory
  import scala.concurrent.duration._

  // переопределим часть конфигурации секцией "client"
  val rootConfig = ConfigFactory.load()
  val config = rootConfig.getConfig("client").withFallback(rootConfig)

  // создадим актор систему и актора-клиента
  val actorSystem = ActorSystem("client-system", config)
  val client: ActorRef = actorSystem.actorOf(Props[Client])

  // полный akka-путь к Storage
  val storagePath = "akka.tcp://storage-system@127.0.0.1:2552/user/storage"

  val storageSelection = actorSystem.actorSelection(storagePath)

  // ждем ответа
  val resolveTimeout = FiniteDuration(10, SECONDS)

  storageSelection.resolveOne(resolveTimeout).foreach { (storage: ActorRef) =>
    // командуем клиенту присоединиться к хранилищу
    println(s"Connected to $storage")
    client ! Client.Connect(storage)
  } (actorSystem.dispatcher) // контекст в которым выполнится Future
}


class Storage extends Actor {
  // перейдем в начальное состояние
  override def receive: Receive = process(Map.empty)

  def process(store: Map[String, String]): Receive = {
    // в ответ на сообщение Get вернем значение ключа в текущем состоянии
    // актор-отправитель сообщения доступен под именем sender
    case Storage.Get(key) => sender ! Storage.GetResult(key, store.get(key))

    // в ответ на сообщение Put перейдем в следующее состояние и отправим подтверждение вызывающему
    case Storage.Put(key, value) =>
      context become process(store + (key -> value))
      sender ! Storage.Ack

    case Storage.Delete(key) =>
      context become process(store - key)
      sender ! Storage.Ack
  }
}

object Storage {
  // in
  final case class Get(key: String)
  final case class Put(key: String, value: String)
  final case class Delete(key: String)

  // out
  final case class GetResult(key: String, value: Option[String])
  case object Ack
}
