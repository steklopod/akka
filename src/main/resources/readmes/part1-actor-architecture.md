## Часть 1: Архитектура актора

>Добавьте в свой проект следующую зависимость:
```scala
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.17"
```

### Введение

**Использование Akka избавляет вас от создания инфраструктуры для системы акторов и от написания низкоуровневого кода, 
необходимого для управления базовым поведением.** _Чтобы оценить это, давайте посмотрим на отношения между действующими вами 
субъектами в вашем коде и теми, которые Akka создает и управляет для вас внутренне, жизненный цикл актора и обработку отказа._

### Иерархия акторов в Акка

Актор в Акке всегда принадлежит родителям. Как правило, вы создаете актора, вызывая **`context.actorOf()`**. Вместо того, 
чтобы создавать «независимого» актора, это создает нового актора в качестве ребенка в уже существующее дерево: создатель-актор 
становится родителем только что созданного дочернего актора. Тогда вы можете спросить, кто является родителем первого 
созданного вами актора?

Как показано ниже, у всех участников есть общий родитель, пользователь-хранитель. С помощью `system.actorOf()` можно создавать 
новые экземпляры акторов под этим актором. Как мы рассмотрели в Руководстве по быстрому запуску, создание актора возвращает 
ссылку, которая является допустимым URL. Так, например, если мы создадим актора с именем `someActor` с 
**`system.actorOf (..., «someActor»)`**, его ссылка будет включать путь `/user/someActor`.

![alt text](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/images/actor_top_tree.png "actor_top_tree")

На самом деле, прежде чем вы создадите актора в своем коде, **Akka уже создал трех действующих лиц в системе**. Имена 
этих встроенных акторов содержат опекуна (**`guardian`**), потому что они контролируют каждого дочернего актора на своем пути. 
Акторы-опекуны (`guardian actors`) включают:

* `/root` guardian (корневой опекун). Это родительский элемент всех участников системы, а последний 
останавливается, когда сама система завершается;

* `/user`  guardian (пользовательский опекун). Это родительский актор для всех созданных пользователем участников. 
Не допускайте, чтобы имя пользователя вас путало, оно не имеет ничего общего с конечными пользователями и с пользовательской 
обработкой. Каждый актор, созданный с использованием библиотеки Akka, будет иметь постоянный путь `/user/` добавленный к нему;

* `/system` guardian (системный опекун).

В примере `AkkaQuickstart` мы уже видели, как `system.actorOf()` создает актора непосредственно под `/user`. Мы называем 
это `актором верхнего уровня (top level actor)`, хотя на практике он находится только на вершине определяемой пользователем 
иерархии. У вас обычно есть только один (или очень немногие) акторы верхнего уровня в вашей `ActorSystem`. Мы создаем 
дочерних или неактивных участников, вызывая `context.actorOf()` от существующего актора. Метод `context.actorOf()` имеет 
подпись, идентичную `system.actorOf()`, ее аналог верхнего уровня.

Самый простой способ увидеть иерархию акторов в действии - распечатать экземпляры `ActorRef`. В этом небольшом эксперименте 
мы создаем актора, печатаем его ссылку, создаем ребенка этого актора и печатаем ссылку на ребенка. Мы начинаем с 
проекта `AkkaQuickstart`.

В проекте `AkkaQuickstart` перейдите к пакету `ru.sample` и запустите в консоли: 
 
 ```sbtshell
    cd C:\projects\Akka
    sbt 
    runMain ru.example.ActorHierarchyExperiments
 ```
 
 >класс PrintMyActorRefActor:
 
```scala
package ru.sample

import akka.actor.{ Actor, Props, ActorSystem }
import scala.io.StdIn

class PrintMyActorRefActor extends Actor {
  override def receive: Receive = {
    case "printit" ⇒
      val secondRef = context.actorOf(Props.empty, "second-actor")
      println(s"Second: $secondRef")
  }
}

object ActorHierarchyExperiments extends App {
  val system = ActorSystem("testSystem")

  val firstRef = system.actorOf(Props[PrintMyActorRefActor], "first-actor")
  println(s"First: $firstRef")
  firstRef ! "printit"

  println(">>> Press ENTER to exit <<<")
  try StdIn.readLine()
  finally system.terminate()
}
```
 
Обратите внимание на то, как сообщение просило первого актора выполнить свою работу. Мы отправили сообщение, используя 
ссылку родителя: `firstRef! "Printit"`. Когда код выполняется, вывод включает ссылки для первого актора и ребенка, 
который он создал как часть `printit`. Результат должен выглядеть примерно так:
  
 ```sbtshell
     First:  Actor[akka://testSystem/user/first-actor#-1856311964]
     Second: Actor[akka://testSystem/user/first-actor/second-actor#1225562397]
 ```
 
Обратите внимание на структуру ссылок:

* Оба пути начинаются с `akka://testSystem/`. Поскольку все ссылки на акторы являются действительными URL-адресами, 
`akka:// `является значением поля протокола;

* Далее, как и во Всемирной паутине, URL-адрес идентифицирует систему. В этом примере система называется `testSystem`, 
но это может быть любое другое имя. Если удаленная связь между несколькими системами включена, эта часть URL-адреса 
включает имя хоста, чтобы другие системы могли найти его в сети;

* Поскольку ссылка второго актора включает в себя путь `/first-actor/`, он идентифицирует его как ребенка первого;

* Последняя часть ссылки на актора, `#-1856311964` или `#1225562397` - уникальный идентификатор, который вы можете 
игнорировать в большинстве случаев.

Теперь, когда вы понимаете, как выглядит иерархия акторов, вам может быть интересно: _зачем нам нужна эта иерархия?_ 

Важной ролью иерархии является безопасное управление жизненными циклами акторов. Давайте рассмотрим это дальше и посмотрим,
 как эти знания помогут нам лучше писать код.
 
 ### Жизненный цикл актора
 
 Акторы появляются при создании, а затем по просьбе пользователя они останавливаются. Всякий раз, когда актор останавливается,
  все его дети рекурсивно останавливаются. Такое поведение значительно упрощает очистку ресурсов и помогает избежать утечек,
   таких как вызванные открытыми сокетами и файлами. На самом деле, обычно упущенная сложность при работе с низкоуровневым 
   многопоточным кодом - это управление жизненным циклом различных параллельных ресурсов.
 
 Чтобы остановить актора, рекомендуемый шаблон - вызвать **`context.stop(self)`** внутри актора, чтобы остановить себя, 
 обычно в качестве ответа на какое-то определенное пользователем сообщение остановки или когда актор выполнен с его 
 заданием. Остановка другого актора технически возможна, вызывая `context.stop(actorRef)`, но _**считается неправильной 
 практикой останавливать произвольных участников таким образом**_: попробуйте отправить им сообщение `PoisonPill` или 
 пользовательское сообщение остановки.
 
 API-интерфейс Akka предоставляет много хуков жизненного цикла, которые вы можете переопределить в реализации актора. 
 Наиболее часто используются **`preStart()`** и **`postStop()`**.
 
 * `preStart()` вызывается после того, как актор начал, но прежде чем он обработает свое первое сообщение;
 
 * `postStop()` вызывается непосредственно перед остановкой актора. После этой обработки сообщения не обрабатываются.
 
 Давайте используем перехваты жизненного цикла `preStart()` и `postStop()` в простом эксперименте, чтобы наблюдать за 
 поведением, когда мы останавливаем актора. Во-первых, добавьте в свой проект следующие 2 акторских класса:
 
```scala
class StartStopActor1 extends Actor {
  override def preStart(): Unit = {
    println("first started")
    context.actorOf(Props[StartStopActor2], "second")
  }
  override def postStop(): Unit = println("first stopped")

  override def receive: Receive = { case "stop" ⇒ context.stop(self)  }
}

class StartStopActor2 extends Actor {
  override def preStart(): Unit = println("second started")
  override def postStop(): Unit = println("second stopped")

  // Actor.emptyBehavior is a useful placeholder when we don't want to handle any messages in the actor.
  override def receive: Receive = Actor.emptyBehavior
}
 ```
 
И создайте `main` класс, как показано выше, чтобы запустить акторы, а затем отправить им сообщение `stop`:
 
```scala
    val first = system.actorOf(Props[StartStopActor1], "first")
    first ! "stop"
```

Вы можете снова использовать `sbt` для запуска этой программы. Результат должен выглядеть следующим образом:

```sbtshell
    first started
    second started
    second stopped
    first stopped
```

Когда мы сначала остановили актора, он остановил своего дочернего актора, во-вторых, прежде чем остановиться. Это 
упорядочение строгое, все перехваты `postStop()` дочерних элементов вызываются до вызова `postStop()` для родителя.

Раздел [Actor Lifecycle](https://doc.akka.io/docs/akka/current/actors.html#actor-lifecycle) справочного руководства Akka 
содержит подробную информацию о полном списке хуков жизненного цикла акторов.

## Обработка ошибок

Родители и дети связаны на протяжении всего жизненного цикла. Всякий раз, когда актор терпит неудачу, он временно 
приостанавливается. Как упоминалось ранее, информация об отказе распространяется на родителя, которая затем решает, 
как обрабатывать исключение, вызванное дочерним актором. Таким образом, родители выступают в качестве надзирателей для 
своих детей. Стратегия супервизора по умолчанию - остановить и перезагрузить ребенка. Если вы не измените 
стратегию по умолчанию, все сбои приведут к перезапуску.

Давайте рассмотрим стратегию по умолчанию в простом эксперименте. Добавьте в свой проект следующие классы:

```scala
    class SupervisingActor extends Actor {
      val child = context.actorOf(Props[SupervisedActor], "supervised-actor")
    
      override def receive: Receive = {
        case "failChild" ⇒ child ! "fail"
      }
    }
    
    class SupervisedActor extends Actor {
      override def preStart(): Unit = println("supervised actor started")
      override def postStop(): Unit = println("supervised actor stopped")
    
      override def receive: Receive = {
        case "fail" ⇒
          println("supervised actor fails now")
          throw new Exception("I failed!")
      }
    }
```

И зупустите это:

```scala
    val supervisingActor = system.actorOf(Props[SupervisingActor], "supervising-actor")
    supervisingActor ! "failChild"
```

Вы должны увидеть результат, похожий на следующий:

```scala
    supervised actor started
    supervised actor fails now
    supervised actor stopped
    supervised actor started
    [ERROR] [09/29/2018 10:47:14.150] [testSystem-akka.actor.default-dispatcher-2] [akka://testSystem/user/supervising-actor/supervised-actor] I failed!
    java.lang.Exception: I failed!
            at example.SupervisedActor$$anonfun$receive$4.applyOrElse(ActorHierarchyExperiments.scala:57)
            ...
```

Мы видим, что после провала контролируемый актер прекращается и сразу же перезапускается. Мы также видим запись в журнале,
 в которой сообщается об исключении, которое было обработано, в данном случае, нашем тестовом исключении. В этом примере 
 мы использовали крючки `preStart()` и `postStop()`, которые по умолчанию будут вызываться после и перед перезапуском, 
 поэтому мы не можем отличить внутри актера от того, был ли он запущен в первый раз или перезапущен. Обычно это правильно, 
 цель перезапуска заключается в том, чтобы установить актера в хорошо известном состоянии, что обычно означает чистую 
 начальную стадию. Однако на самом деле происходит то, что вызываются методы `preRestart()` и `postRestart()`, которые, 
 если не переопределены, по умолчанию делегируют `postStop()` и `preStart()` соответственно. Вы можете поэкспериментировать 
 с переопределением этих дополнительных методов и посмотреть, как изменяется результат.

Для нетерпеливых мы также рекомендуем посмотреть [справочную страницу контроля](https://doc.akka.io/docs/akka/current/general/supervision.html).


_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)