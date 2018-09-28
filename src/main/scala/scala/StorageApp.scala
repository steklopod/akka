package scala
import akka.actor._

// все акторы наследуются от базового класса akka.actor.Actor
class Storage extends Actor {
  // функция обработчик сообщений
  override def receive: Receive = {
    case msg => println(msg)
  }
}

object StorageApp extends App {
  // все акторы принадлежат одной из систем акторов
  val actorSystem = ActorSystem()

  // создавать акторы надо вызовом actorOf на соответствующем
  // actorSystem или context.actorOf внутри самого актора
  val storageActor: ActorRef = actorSystem.actorOf(Props[Storage])

  // методы акторов не вызываются напрямую, а только посредством отправки
  // сообщений на инкапсулирующую его ссылку actorRef при помощи .tell или !
  storageActor ! "string message"
  storageActor ! 42

  // дождемся обработки сообщений
  Thread.sleep(1000)

  // выход из программы
  actorSystem.terminate()
}