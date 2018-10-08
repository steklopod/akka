## Часть 2: Создание первого актора

С пониманием иерархии и поведения актора, оставшийся вопрос заключается в том, как сопоставить компоненты верхнего уровня 
нашей системы `IoT` с субъектами. Может возникнуть соблазн сделать акторов, которые представляют устройства и панели 
мониторинга на верхнем уровне. Вместо этого мы рекомендуем создать явный компонент, который представляет все приложение. 
Другими словами, у нас будет один актор верхнего уровня в нашей системе `IoT`. Компонентами, которые создают и управляют 
устройствами и панелями мониторинга, станут дети этого актора. Это позволяет нам реорганизовать диаграмму архитектуры 
примера использования в дерево акторов:

![alt text](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/images/arch_tree_diagram.png "arch_tree_diagram")

Мы можем определить первого актора, `IotSupervisor`, несколькими простыми строками кода. Чтобы запустить учебное приложение:

1. Создайте новый исходный файл `IotSupervisor` в пакете `com.lightbend.akka.sample`;

2. Вставьте следующий код в новый файл, чтобы определить `IotSupervisor`.

```scala
    package ru.examples
    import akka.actor.{Actor, ActorLogging, Props}
    
    object IotSupervisor {
      def props(): Props = Props(new IotSupervisor)
    }
    
    class IotSupervisor extends Actor with ActorLogging {
      override def preStart(): Unit = log.info("IoT Application started")
      override def postStop(): Unit = log.info("IoT Application stopped")
    
      // Нет необходимости обрабатывать сообщения
      override def receive = Actor.emptyBehavior
    }
```

Код похож на примеры акторов, которые мы использовали в предыдущих экспериментах, но обратите внимание:

* Вместо `println()` мы используем вспомогательный трэйт `ActorLogging`, который непосредственно вызывает встроенную в 
систему регистрации Akka;

* Мы используем рекомендованный шаблон для создания акторов, определяя метод `props()` в сопутствующем объекте актора.

Чтобы обеспечить основную точку входа, которая создает систему акторов, добавьте следующий код к новому объекту `IotApp`:

```scala
    object IotApp extends App {
      val system = ActorSystem("iot-system")
      try {
        // Create top level supervisor
        val supervisor = system.actorOf(IotSupervisor.props(), "iot-supervisor")
        // Exit the system after ENTER is pressed
        StdIn.readLine()
      } finally {
        system.terminate()
      }
    }
```

Приложение мало что делает, кроме распечатки, что оно запущено. Но у нас есть первый актор, и мы готовы добавить других акторов.


_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)