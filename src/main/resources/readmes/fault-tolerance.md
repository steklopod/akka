## Отказоустойчивость

#### Вступление
Как объясняется в [Системах акторов](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/readmes/concepts/actor-systems.md),
 каждый актор является руководителем своих детей, и поэтому каждый актор определяет стратегию надзора за обработкой ошибок.
  После этого эта стратегия не может быть изменена, поскольку она является неотъемлемой частью структуры акторской системы.

### Обработка ошибок на практике
Во-первых, давайте посмотрим на образец, который иллюстрирует один из способов обработки ошибок хранилища данных, что 
является типичным источником сбоев в реальных приложениях. Конечно, это зависит от фактического приложения, что можно 
сделать, когда хранилище данных недоступно, но в этом примере мы используем подход с перекрестным соединением с наилучшим усилием.

Прочтите следующий исходный код. Встроенные комментарии объясняют разные части обработки ошибок и почему они добавлены. 
Также рекомендуется использовать этот образец, так как легко следить за выходом журнала, чтобы понять, что происходит во 
время выполнения.

### Создание стратегии супервизора
В следующих разделах более подробно объясняется механизм обработки ошибок и альтернативы.

Для демонстрации рассмотрим следующую стратегию:

```scala
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._

override val supervisorStrategy =
  OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: ArithmeticException      ⇒ Resume
    case _: NullPointerException     ⇒ Restart
    case _: IllegalArgumentException ⇒ Stop
    case _: Exception                ⇒ Escalate
  }
```
Мы выбрали несколько известных типов исключений, чтобы продемонстрировать применение директив об ошибках, описанных в 
[Наблюдение и мониторинг](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/readmes/concepts/supervision-and-monitoring.md) 
Во-первых, это стратегия «один-к-одному», а это означает, что каждый ребенок рассматривается отдельно (стратегия 
«все-для-одного» работает очень аналогично, с той лишь разницей, что любое решение применяется ко всем детям супервизора
 не только неудачный). В приведенном выше примере `10` и `1 minute` передаются параметрам `maxNrOfRetries` и внутри `TimeRange`
  соответственно, что означает, что стратегия перезапускает ребенка до 10 перезапуска в минуту. Ребенок-актор 
  останавливается, если счет перезапуска превышает `maxNrOfRetries` в течение `withinTimeRange` продолжительности.

Кроме того, для этих параметров есть специальные значения. Если вы укажете:

* `-1` до `maxNrOfRetries` и `Duration.Inf` до `WithinTimeRange`
   * то ребенок всегда перезапускается без каких-либо ограничений
* `-1` до `maxNrOfRetries`, а не бесконечное значение `Duration` до `withinTimeRange`
   * `maxNrOfRetries` обрабатывается как `1`
* неотрицательное число до `maxNrOfRetries` и `Duration.Inf` до `WithinTimeRange`
   * `withinTimeRange` рассматривается как бесконечная продолжительность (то есть) независимо от того, сколько времени
    потребуется, после того, как счет перезапуска превышает `maxNrOfRetries`, дочерний актор остановлен

Операция соответствия, которая составляет основную часть тела
имеет тип `Decide`r, который является `PartialFunction[Throwable, Directive]`. Это часть, которая отображает типы 
отказов детей в соответствии с их соответствующими директивами.

>Если стратегия объявлена ​​внутри контролирующего субъекта (в противоположность внутри сопутствующего объекта), ее 
решающий элемент имеет доступ ко всем внутренним состояниям актора в потокобезопасном режиме, включая получение ссылки
 на текущий неудавшийся ребенок (доступный как отправитель сообщения об ошибке).
 
#### Стратегия супервизора по умолчанию
`Escalate` используется, если определенная стратегия не распространяется на исключение, которое было выбрано.

Когда стратегия супервизора не определена для актора, по умолчанию обрабатываются следующие исключения:

* `ActorInitializationException` остановит неудачного дочернего актора
* `ActorKilledException` остановит неудачного дочернего актора
* `DeathPactException` остановит неудачного дочернего актора
* `Exception` будет перезапускать неудачного дочернего актора
* Другие типы `Throwable` будут увеличены до родительского актора

Если исключение перерастает весь путь до корневого опекуна, оно будет обрабатывать его так же, как стратегия по 
умолчанию, определенная выше.

Вы можете комбинировать свою стратегию со стратегией по умолчанию:

```scala
import akka.actor.OneForOneStrategy
import akka.actor.SupervisorStrategy._
import scala.concurrent.duration._

override val supervisorStrategy =
  OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
    case _: ArithmeticException ⇒ Resume
    case t ⇒
      super.supervisorStrategy.decider.applyOrElse(t, (_: Any) ⇒ Escalate)
  }
```

#### Прекращение стратегии супервизора
Ближе к пути Erlang - стратегия остановить детей, когда они терпят неудачу, а затем предпринять корректирующие действия
 в супервизоре, когда `DeathWatch` сигнализирует о потере ребенка. Эта стратегия также предоставляется предварительно
  упакованной как `SupervisorStrategy.stoppingStrategy` с сопровождающим конфигуратором `StoppingSupervisorStrategy`,
   который будет использоваться, если вы хотите, чтобы опекун `/user` применил его.

#### Регистрация сбоев акторов
По умолчанию `SupervisorStrategy` регистрирует сбои, если они не эскалированы. Предполагается, что сгенерированные отказы
 будут обрабатываться и потенциально регистрироваться на уровне выше в иерархии.

Вы можете отключить ведение журнала по умолчанию в `SupervisorStrategy`, установив `loggingEnabled` в `false` при его 
создании. Индивидуальное ведение журнала может быть выполнено внутри `Decider`. Обратите внимание, что ссылка на текущий
 неактивный ребенок доступна в качестве `sender`, когда `SupervisorStrategy` объявляется внутри контролирующего участника.

Вы также можете настроить ведение журнала в своей собственной реализации `SupervisorStrategy`, переопределив метод `logFailure`.

### Надзор за исполнителями на высшем уровне
Акторы `Toplevel` означают те, которые создаются с использованием `system.actorOf()`, и они являются дочерними 
элементами пользовательского стража. В этом случае не применяются специальные правила, опекун применяет 
сконфигурированную стратегию.

### Тест-приложение
В следующем разделе показаны эффекты различных директив на практике, когда необходима тестовая установка. Прежде всего,
 нам нужен подходящий руководитель:

```scala
import akka.actor.{Actor, Props}

class Supervisor extends Actor {
  import akka.actor.OneForOneStrategy
  import akka.actor.SupervisorStrategy._
  import scala.concurrent.duration._

  override val supervisorStrategy =
    OneForOneStrategy(maxNrOfRetries = 10, withinTimeRange = 1 minute) {
      case _: ArithmeticException      ⇒ Resume
      case _: NullPointerException     ⇒ Restart
      case _: IllegalArgumentException ⇒ Stop
      case _: Exception                ⇒ Escalate
    }

  def receive = {
    case p: Props ⇒ sender() ! context.actorOf(p)
  }
}
```

Этот супервизор будет использоваться для создания ребенка, с которым мы можем экспериментировать:

```scala
import akka.actor.Actor

class Child extends Actor {
  var state = 0
  def receive = {
    case ex: Exception ⇒ throw ex
    case x: Int        ⇒ state = x
    case "get"         ⇒ sender() ! state
  }
}
```
Тест проще с помощью утилит, описанных в `Testing Actor Systems`, где `TestProbe` предоставляет акторский справочник, 
полезный для получения и проверки ответов.

```scala
import akka.actor.ActorSystem
import akka.testkit.{ImplicitSender, TestKit}
import com.typesafe.config.ConfigFactory
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpecLike}

class FaultHandlingDocSpec(_system: ActorSystem) extends TestKit(_system)
  with ImplicitSender with WordSpecLike with Matchers with BeforeAndAfterAll {

  def this() = this(ActorSystem(
    "FaultHandlingDocSpec",
    ConfigFactory.parseString("""
      akka {
        loggers = ["akka.testkit.TestEventListener"]
        loglevel = "WARNING"
      }
      """)))

  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A supervisor" must {
    "apply the chosen strategy for its child" in {
      // code here
    }
  }
}
```

Давайте создадим акторов:

```scala
val supervisor = system.actorOf(Props[Supervisor], "supervisor")

supervisor ! Props[Child]
val child = expectMsgType[ActorRef] // получить ответ от TestKit’s testActor
```
Первый тест должен продемонстрировать директиву `Resume`, поэтому мы попробуем его, установив какое-то не начальное 
состояние у актера и получим ошибку:

```scala
child ! 42 // set state to 42
child ! "get"
expectMsg(42)

child ! new ArithmeticException // crash it
child ! "get"
expectMsg(42)
```

[Подробнее](https://doc.akka.io/docs/akka/current/fault-tolerance.html)

_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)