## Распределенные системы. Akka.

### Модель акторов
Последние десятилетия показали, что распределенные системы и многозадачность в классическом императивном подходе - это 
очень сложная задача. Возможно, именно это дало начало очередному витку бума функциональных языков/фич и неизменяемых структур данных.

Один из подходов к решению проблем построения распределенных систем предлагает [модель акторов](https://ru.wikipedia.org/wiki/модель_акторов)
 (actor model) пришедшая из языка `Erlang` разработанного компанией Ericsson для телекоммуникаций. Кроме того, на языке Erlang построен Whatsapp.

Вкратце описать модель акторов можно как систему состоящую из одновременно функционирующих объектов, коммуникация между 
которыми происходит исключительно обменом сообщениями. В каком-то роде можно рассматривать ее как самую чистую ООП-модель, 
где каждый объект функционирует одновременно с остальными.

**Актор** является вычислительной сущностью, которая в ответ на полученное сообщение может одновременно:

* отправить конечное число сообщений другим акторам;

* создать конечное число новых акторов;

* выбрать тип поведения, которое будет использоваться для следующего сообщения в свой адрес.

В скала модель акторов представлена библиотекой [akka](http://akka.io/).

>Пример

_Рассмотрим построение небольшого приложения при помощи akka, построив простое хранилище данных ключ-значение 
(in-memory key-value store) и создав для нее несколько клиентов._

Создадим обычную структуру sbt-проекта:

```scala
    build.sbt
    src/
      main/
        resources/
        scala/
          StorageApp.scala
```

Собственно подключим akka в файле сборки:

**build.sbt**

```scala
    libraryDependencies ++= Seq( "com.typesafe.akka" %% "akka-actor" % "2.5.16" )
    resolvers ++= Seq( Classpaths.typesafeReleases )
```

Теперь опишем простейший актор для нашего хранилища, который будет выводить на печать все приходящие сообщения:

```scala
    import akka.actor._
    
    // все акторы наследуются от базового класса akka.actor.Actor
    class Storage extends Actor {
      override def receive: Receive = {      // функция обработчик сообщений
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
```

Каждый актор имеет очередь сообщений, откуда он достает их на обработку строго по одному и передает функции `receive`
 которая должна быть реализована в рамках трех пунктов перечисленных выше.

Тип функции `receive: PartialFunction[Any, Unit]`, то есть, это по сути функция `Any => Unit`, которая может быть не 
определена на каком-то наборе значений.

Опишем сообщения, которые может принимать наш `Storage`. Одним из принятых соглашений является помещение их в 
компаньон-объект актора:

```scala
    object Storage {
      // in
      final case class Get(key: String)
      final case class Put(key: String, value: String)
      final case class Delete(key: String)
    
      // out
      final case class GetResult(key: String, value: Option[String])
      case object Ack
    }
```

Так как все общение между акторами происходит посредством передачи сообщений, то нельзя быть уверенным в том, что актор получил 
сообщения пока он не пришлет в ответ подтверждение. Поэтому для большинства входных сообщений имеет смысл описывать сообщения-ответы.

Построение программ следуя этой парадигме довольно необычно и требует некоторого переосмысления, но если вдуматься, то
 оно естественно для реального мира. Систему акторов можно моделировать представляя их как группу людей, которые голосом 
 передают друг другу команды. Если команда важная, то желательно получить подтверждение того, что ее услышали, что человек
  еще жив и т.д.

Благодаря этому актор может безопасно хранить в себе состояние в многопоточной среде и менять его выбирая новый тип 
поведения в ответ на полученное сообщение вызовом метода context.become и описав функцию обработки сообщений для нового состояния.

Посмотрим, как наш актор Storage будет держать в себе состояние хранилища данных:

```scala
   class Storage extends Actor {
   
     // перейдем в начальное состояние
     override def receive: Receive = process(Map.empty)
   
     def process(store: Map[String, String]): Receive = {
   
       // в ответ на сообщение Get вернем значение ключа в текущем состоянии
       // актор-отправитель сообщения доступен под именем sender
       case Storage.Get(key) =>
         sender ! Storage.GetResult(key, store.get(key))
   
       // в ответ на сообщение Put перейдем в следующее состояние
       // и отправим подтверждение вызывающему
       case Storage.Put(key, value) =>
         context become process(store + (key -> value))
         sender ! Storage.Ack
   
       // аналогично
       case Storage.Delete(key) =>
         context become process(store - key)
         sender ! Storage.Ack
     }
   }
```

## Клиенты

Для проверки функционала создадим актор-клиент, который будет считывать команды из консоли и отправлять хранилищу.


```scala
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
         readLine().split(' ') match {
           case Array("get", key) => storage ! Storage.Get(key)
           case Array("put", key, value) => storage ! Storage.Put(key, value)
           case Array("delete", key) => storage ! Storage.Delete(key)
           case Array("stop") => context.system.terminate()
           case _ => println("Unknown command")
         }
   
         Thread.sleep(100)     // дадим время обработать сообщение
         self ! Client.Process // "рекурсия"
   
       // прием ответов от хранилища
       case Storage.GetResult(key, value) => println(s"Received: $key -> $value")
   
       case Storage.Ack => println("Received ack.")
     }
   }
```

Запустим оба актора:

```scala
   object StorageApp extends App {
     // все акторы принадлежат одной из систем акторов
     val actorSystem = ActorSystem()
   
     val storage: ActorRef = actorSystem.actorOf(Props[Storage])
   
     val client: ActorRef = actorSystem.actorOf(Props[Client])
   
     client ! Client.Connect(storage)
   }
```



_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._
