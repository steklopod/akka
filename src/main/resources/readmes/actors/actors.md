## Акторы

### Вступление
Модель Actor обеспечивает более высокий уровень абстракции для написания параллельных и распределенных систем. Это 
позволяет разработчику отказаться от явной блокировки и управления потоками, что упрощает создание правильных 
параллельных и параллельных систем. Акторы были определены в газете Карла Хьюитта в 1973 году, но были популяризированы 
языком Erlang и использовались, например, в Эрикссон с большим успехом для создания высококонкурентных и надежных 
телекоммуникационных систем.

### Создание акторов

#### Определение класса Actor
Акторы реализуются путем расширения базового типажа Актора и реализации метода `receive`. Метод **`receive`** должен определять 
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
`akka.actor.UnhandledMessage` (`message`, `sender`, `recipient`) будет опубликован в `ActorSystem`s `EventStream`.

Обратите внимание, что возвращаемый тип поведения, определенный выше, - `Unit`; если актор должен ответить на полученное сообщение, это должно быть сделано явно, как описано ниже.

Результатом метода `receive` является объект частичной функции, который хранится внутри актора в качестве его 
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

#### Реквизит (Props)
`Props` - это класс конфигурации, который указывает параметры для создания участников, рассматривая его как непреложный 
и, следовательно, свободно распространяемый рецепт создания актора, включая связанную информацию о развертывании 
(например, какой диспетчер использовать). Вот несколько примеров создания экземпляра реквизита.

```scala
import akka.actor.Props

val props1 = Props[MyActor]
val props2 = Props(new ActorWithArgs("arg")) // осторожно, см. ниже
val props3 = Props(classOf[ActorWithArgs], "arg") // нет поддержки `value class arguments`
```

Второй вариант показывает, как передать аргументы конструктора создаваемому Актору, но его следует использовать только 
вне участников, как описано ниже.

В последней строке показана возможность передавать аргументы конструктора независимо от используемого им контекста. 
Наличие конструктора соответствия проверяется во время построения объекта Props, в результате чего возникает исключение 
`IllegalArgumentException`, если найдены конструкторы с отсутствием или множественным соответствием.

>Рекомендуемый подход к созданию Props акторане поддерживается для случаев, когда конструктор-актор принимает 
классы значений в качестве аргументов.

#### Опасные варианты

```scala
//  НЕ РЕКОМЕНДУЕТСЯ в рамках другого актора: призывает закрыть закрывающий класс
val props7 = Props(new MyActor)
```

Этот метод не рекомендуется использовать в рамках другого актора, потому что он поощряет закрытие охватывающей области, 
что приводит к несериализуемым `Props` и возможно условиям гонки (нарушая инкапсуляцию актора). С другой стороны, использование
 этого варианта на фабрике Props в сопутствующем объекте актора, как описано в «Рекомендуемой практике» ниже, полностью 
 прекрасное.

Для этих методов использовалось два варианта использования: передача аргументов конструктора актору, который решается 
недавно введенным методом `Props.apply(clazz, args)` выше или рекомендуемой практикой ниже, и созданием акторов «на месте»
 как анонимных классы. Последнее должно быть решено, вместо того, чтобы вместо этого использовать эти классы с именами
  классов (если они не объявлены в объекте верхнего уровня, тогда эта ссылка на экземпляр экземпляра должна быть 
  передана в качестве первого аргумента).
  
>Предупреждение!  Объявление одного актора в другом очень опасно и разрушает инкапсуляцию актора. Никогда не 
передавайте эту ссылку актору в реквизиты!  

#### Крайние случаи (Edge cases)
В создании актора есть два крайних случая:

* Актор с аргументами `AnyVal`.

```scala
case class MyValueClass(v: Int) extends AnyVal
```

```scala
class ValueActor(value: MyValueClass) extends Actor {
  def receive = {
    case multiplier: Long ⇒ sender() ! (value.v * multiplier)
  }
}
val valueClassProp = Props(classOf[ValueActor], MyValueClass(5)) // Unsupported
```

* Актор со значениями конструктора по умолчанию.

```scala
class DefaultValueActor(a: Int, b: Int = 5) extends Actor {
  def receive = {
    case x: Int ⇒ sender() ! ((a + x) * b)
  }
}

val defaultValueProp1 = Props(classOf[DefaultValueActor], 2.0) // Unsupported

class DefaultValueActor2(b: Int = 5) extends Actor {
  def receive = {
    case x: Int ⇒ sender() ! (x * b)
  }
}
val defaultValueProp2 = Props[DefaultValueActor2] // Unsupported
val defaultValueProp3 = Props(classOf[DefaultValueActor2]) // Unsupported
```

В обоих случаях исключение `IllegalArgumentException` будет брошено, из-за того что никакой конструктор соответствия не найден.

В следующем разделе объясняются рекомендуемые способы создания реквизита Актора таким образом, который одновременно 
защищает от этих крайних случаев.

#### Рекомендуемая практика
Это хорошая идея, чтобы предоставить фабричные методы на сопутствующем объекте каждого Актора, которые помогают 
поддерживать создание подходящих реквизитов как можно ближе к определению актора. Это также позволяет избежать ошибок, 
связанных с использованием метода `Props.apply(...)`, который принимает аргумент `by-name`, поскольку внутри 
объекта-компаньона данный блок кода не сохранит ссылку на свою охватывающую область:

```scala
object DemoActor {
  /**
    * Создайте реквизит для актора этого типа.
    *
    * @param magicNumber - Магическое число, которое нужно передать конструктору этого актора.
    * @ возвратите реквизит для создания этого актора, который затем может быть дополнительно настроен
    * (например, вызов `.withDispatcher ()` на нем)
   */
  def props(magicNumber: Int): Props = Props(new DemoActor(magicNumber))
}

class DemoActor(magicNumber: Int) extends Actor {
  def receive = {
    case x: Int ⇒ sender() ! (x + magicNumber)
  }
}

class SomeOtherActor extends Actor {
  // Props(new DemoActor(42)) не будет безопасным
  context.actorOf(DemoActor.props(42), "demo")
  // ...
}
```

Другая хорошая практика заключается в том, чтобы объявить, какие сообщения может получить Актор в сопутствующем 
объекте Актора, что облегчает понимание того, что он может получить:

```scala
object MyActor {
  case class Greeting(from: String)
  case object Goodbye
}
class MyActor extends Actor with ActorLogging {
  import MyActor._
  def receive = {
    case Greeting(greeter) ⇒ log.info(s"I was greeted by $greeter.")
    case Goodbye           ⇒ log.info("Someone said goodbye to me.")
  }
}
```

#### Создание акторов с реквизитом
Акторы создаются путем передачи экземпляра Props в заводский метод `actorOf`, который доступен в `ActorSystem` и `ActorContext`.

```scala
import akka.actor.ActorSystem

// ActorSystem - это тяжелый объект: создавайте только одно на все приложение
val system = ActorSystem("mySystem")
val myActor = system.actorOf(Props[MyActor], "myactor2")
```

Использование `ActorSystem` создаст игроков верхнего уровня, контролируемых действующим акторским актором, в то время как 
использование контекста актора создаст дочернего актора.

```scala
class FirstActor extends Actor {
  val child = context.actorOf(Props[MyActor], name = "myChild")
  def receive = {
    case x ⇒ sender() ! x
  }
}
```

Рекомендуется создавать иерархию детей, внуков и т.д., чтобы она соответствовала логической структуре обработки отказа 
приложения.

Вызов `actorOf` возвращает экземпляр `ActorRef`. Это дескриптор экземпляра актора и единственный способ взаимодействия 
с ним. `ActorRef` неизменен (` immutable`) и имеет отношения «один к одному» с представленным им Актором. `ActorRef`
 также сериализуется и поддерживается сетью. Это означает, что вы можете сериализовать его, отправить его по проводу и 
 использовать его на удаленном хосте, и он все равно будет представлять одного и того же Актора на исходном узле по всей сети.

Параметр имени является необязательным, но вы должны предпочтительно назвать своих участников, поскольку это используется
 в сообщениях журнала и для идентификации участников. Имя не должно быть пустым или начинаться с $, но оно может содержать
  URL-кодированные символы (например, `%20` для пробела). Если данное имя уже используется другим дочерним элементом 
  для одного и того же родителя, генерируется `InvalidActorNameException`.

Акторы автоматически запускаются асинхронно при создании.

#### Классы значений как аргументы конструктора
Рекомендуемый способ создания экземпляров акторских реквизитов использует отражение во время выполнения, чтобы определить
 правильный вызывающий конструктор, и из-за технических ограничений не поддерживается, когда указанный конструктор 
 принимает аргументы, которые являются классами значений. В этих случаях вы должны либо распаковать аргументы, либо 
 создать реквизит, вызвав конструктор вручную:

```scala
class Argument(val value: String) extends AnyVal
class ValueClassActor(arg: Argument) extends Actor {
  def receive = { case _ ⇒ () }
}

object ValueClassActor {
  def props1(arg: Argument) = Props(classOf[ValueClassActor], arg) // fails at runtime
  def props2(arg: Argument) = Props(classOf[ValueClassActor], arg.value) // ok
  def props3(arg: Argument) = Props(new ValueClassActor(arg)) // ok
}
```

#### Внедрение зависимости (Dependency Injection)
Если у вашего актора есть конструктор, который принимает параметры, то они также должны быть частью реквизита (Props), как 
описано выше. Но бывают случаи, когда должен использоваться заводский метод, например, когда фактические аргументы 
конструктора определяются каркасом внедрения зависимостей.

```scala
import akka.actor.IndirectActorProducer

class DependencyInjector(applicationContext: AnyRef, beanName: String)
  extends IndirectActorProducer {

  override def actorClass = classOf[Actor]
  override def produce =
    new Echo(beanName)

  def this(beanName: String) = this("", beanName)
}

val actorRef = system.actorOf(
  Props(classOf[DependencyInjector], applicationContext, "hello"),
  "helloBean")
```

>Предупреждение! Иногда у вас может возникнуть соблазн предложить `IndirectActorProducer`, который всегда возвращает тот
 же экземпляр, например. используя `lazy val`. Это не поддерживается, поскольку это противоречит смыслу рестарта 
 актора. При использовании инверсии зависимостей, акторские бины НЕ ДОЛЖНЫ иметь синглтон скоуп.


#### Inbox
При написании кода вне участников, который должен общаться с участниками, шаблон запроса (`ask` паттерн) может быть решением 
(см. ниже), но есть две вещи, которые он не может сделать: получение нескольких ответов (например, подписка на `ActorRef`
 на службу уведомлений) и просмотр других жизненный цикл акторов. Для этих целей есть класс `Inbox`:
 
 ```scala
implicit val i = inbox()
echo ! "hello"
i.receive() should ===("hello")
```

Существует неявное преобразование из Inbox в ссылку «Актор», что означает, что в этом примере ссылка отправителя будет 
таковой у субъекта, скрытого в Inbox. Это позволяет получить ответ на последней строке. Наблюдение за актором довольно просто:

```scala
val target = // some actor
val i = inbox()
i watch target
```



_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)