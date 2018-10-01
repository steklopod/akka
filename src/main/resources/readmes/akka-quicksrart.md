## Что делает `Hello World`?

Пример `Hello World` иллюстрирует основы `Akka`. В течение 30 минут 
вы сможете загрузить и запустить пример и использовать это руководство, чтобы понять, как построен этот пример. 

> Чтобы запустить `Hello World`:

1. В консоли измените каталоги на верхний уровень распакованного проекта: 
 
 ```sbtshell
    cd C:\projects\Akka
 ```
2. Чтобы запустить [sbt](https://www.scala-sbt.org/1.x/docs/index.html) (_`sbt` загружает зависимости проекта_) в Windows введите:
```sbtshell
    sbt.bat
```
3. В командной строке `sbt` введите 
 ```sbtshell
     run
 ```
 _`sbt` создает проект и запускает `Hello World`._

>Результат должен выглядеть примерно так (прокрутите весь вправо, чтобы увидеть вывод `Actor`а):
```text
    [info] Compiling 1 Scala source to C:...\Akka\target\scala-2.12\classes ...
    [info] Done compiling.
    [info] Packaging C:\...\Akka\target\scala-2.12\akka_2.12-0.1.jar ...
    [info] Done packaging.
    [info] Running ru.example.AkkaQuickstart
    [INFO] [10/01/2018 12:59:05.158] [helloAkka-akka.actor.default-dispatcher-5] [akka://helloAkka/user/printerActor] Greeting received (from Actor[akka://helloAkka/user/helloGreeter#1413285717]): Hello, Scala
    [INFO] [10/01/2018 12:59:05.158] [helloAkka-akka.actor.default-dispatcher-5] [akka://helloAkka/user/printerActor] Greeting received (from Actor[akka://helloAkka/user/howdyGreeter#338475647]): Howdy, Akka
    [INFO] [10/01/2018 12:59:05.158] [helloAkka-akka.actor.default-dispatcher-5] [akka://helloAkka/user/printerActor] Greeting received (from Actor[akka://helloAkka/user/goodDayGreeter#-623443556]): Good day, Play
    [INFO] [10/01/2018 12:59:05.158] [helloAkka-akka.actor.default-dispatcher-5] [akka://helloAkka/user/printerActor] Greeting received (from Actor[akka://helloAkka/user/howdyGreeter#338475647]): Howdy, Lightbend
```

Как вы видели на консольном выводе, в примере выводится несколько приветствий. Давайте посмотрим, что происходит во время выполнения.

![alt text](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/hello-akka-architecture.png "hello-akka-architecture")

1. Во-первых, основной класс (`main class`) создает `akka.actor.ActorSystem`, **контейнер, в котором работают акторы**; 

2. Затем он создает **три экземпляра актора** `Greeter` и **один экземпляр актора** `Printer`;

3. Затем пример **отправляет сообщения в экземпляры** актора`Greater`, которые хранят их внутри;

4. Наконец, инструкция команд для акторов `Greeter` **активируют их для отправки сообщений** в актор `Printer`, 
который выводит их на консоль:

![alt text](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/hello-akka-messages.png "hello-akka-messages")

Давайте рассмотрим некоторые рекомендации по работе с акторами и сообщениями в контексте примера `Hello World`.

[=> далее: Определение акторов и сообщений](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/readmes/defining-actors-and-messages.md)

_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)
