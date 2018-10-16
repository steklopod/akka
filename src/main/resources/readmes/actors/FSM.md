## FSM

### Обзор
`FSM (конечный автомат)` доступен в качестве микширования для актера Акка и лучше всего описывается в принципах 
проектирования Erlang.

FSM можно охарактеризовать как набор соотношений вида:

> **Состояние(S) x Событие(E) -> Действия(A), Состояние(S')**

Эти отношения интерпретируются как значения:

>Если мы находимся в `Состоянии S` и возникает `Событие E`, мы должны выполнить `Действия A` и сделать переход к `Состоянию S'`.

### Простой пример
Чтобы продемонстрировать большинство функций признака `FSM`, рассмотрите актера, который должен получать и отправлять 
сообщения в очередь, когда они поступают в пакет, и отправлять их после окончания пакета или получения запроса на флеш.

Во-первых, рассмотрите все нижеприведенные инструкции для импорта:

```scala
import akka.actor.{ ActorRef, FSM }
import scala.concurrent.duration._
```

Контракт нашего актера «Buncher» заключается в том, что он принимает или производит следующие сообщения:

```scala
// полученные события
final case class SetTarget(ref: ActorRef)
final case class Queue(obj: Any)
case object Flush

// отправленные события
final case class Batch(obj: immutable.Seq[Any])
```

`SetTarget` необходим для его запуска, установка адресата для передачи партий(Batches); Очередь (Queue) добавит во 
внутреннюю очередь, в то время как `Flush` отметит конец пакета.

```scala
sealed trait State
case object Idle extends State
case object Active extends State

sealed trait Data
case object Uninitialized extends Data
final case class Todo(target: ActorRef, queue: immutable.Seq[Any]) extends Data
```

[Подробнее](https://doc.akka.io/docs/akka/current/fsm.html)

_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)