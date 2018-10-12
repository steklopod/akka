## Акторы

### Вступление
Модель Actor обеспечивает более высокий уровень абстракции для написания параллельных и распределенных систем. Это 
позволяет разработчику отказаться от явной блокировки и управления потоками, что упрощает создание правильных 
параллельных и параллельных систем. Акторы были определены в газете Карла Хьюитта в 1973 году, но были популяризированы 
языком Erlang и использовались, например, в Эрикссон с большим успехом для создания высококонкурентных и надежных 
телекоммуникационных систем.

### Создание акторов
>Поскольку Akka обеспечивает родительский контроль, каждый актор контролируется и (потенциально) руководителем своих 
детей, желательно, чтобы вы ознакомились с системами Actor и надзором, а также могли бы помочь прочитать ссылки на 
актора, пути и адреса.

### Определение класса Actor
Акторы реализуются путем расширения базового типажа Актора и реализации метода приема. Метод **`receive`** должен определять 
ряд операторов `case` (который имеет тип `PartialFunction[Any, Unit]`), который определяет, какие сообщения может 
обрабатывать ваш актор, используя стандартное сопоставление шаблонов Scala, а также реализацию того, как сообщения 
должны обрабатываться.

Вот пример:

```scala
import akka.actor.Actor
import akka.actor.Props
import akka.event.Logging

class MyActor extends Actor {
  val log = Logging(context.system, this)

  def receive = {
    case "test" ⇒ log.info("получен тест")
    case _      ⇒ log.info("получено неизвестное сообщение")
  }
}
```

Обратите внимание, что `receive` цикл сообщений является исчерпывающим. Это означает, что вам необходимо предоставить 
соответствие шаблону всем сообщениям, которые оно может принять, и если вы хотите иметь возможность обрабатывать 
неизвестные сообщения, тогда вам нужно иметь случай по умолчанию, как в приведенном выше примере. В противном случае 
ёakka.actor.UnhandledMessageё (`message`, `sender`, `recipient`) будет опубликован в `ActorSystem`s `EventStream`.

Обратите внимание, что возвращаемый тип поведения, определенный выше, - `Unit`; если актор должен ответить на полученное сообщение, это должно быть сделано явно, как описано ниже.

Результатом метода получения является объект частичной функции, который хранится внутри актора в качестве его 
«первоначального поведения».

```scala
import akka.actor.{ ActorSystem, Actor, ActorRef, Props, PoisonPill }
import language.postfixOps
import scala.concurrent.duration._

case object Ping
case object Pong

class Pinger extends Actor {
  var countDown = 100

  def receive = {
    case Pong ⇒
      println(s"${self.path} received pong, count down $countDown")

      if (countDown > 0) {
        countDown -= 1
        sender() ! Ping
      } else {
        sender() ! PoisonPill
        self ! PoisonPill
      }
  }
}

class Ponger(pinger: ActorRef) extends Actor {
  def receive = {
    case Ping ⇒
      println(s"${self.path} received ping")
      pinger ! Pong
  }
}

    val system = ActorSystem("pingpong")

    val pinger = system.actorOf(Props[Pinger], "pinger")

    val ponger = system.actorOf(Props(classOf[Ponger], pinger), "ponger")

    import system.dispatcher
    system.scheduler.scheduleOnce(500 millis) {
      ponger ! Ping
    }
```

_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)