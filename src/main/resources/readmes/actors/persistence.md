## Постоянство (Persistence)

Расширение `Akka Persistence` поставляется с несколькими встроенными плагинами сохранения, включая журнал на основе кучи 
памяти, локальный хранилище снимков на базе файловой системы и журнал на основе LevelDB.

Плагины на основе `LevelDB` потребуют следующую дополнительную зависимость:
```sbtshell
libraryDependencies += "org.fusesource.leveldbjni" % "leveldbjni-all" % "1.8"
```

### Вступление
Постоянство Акки позволяет субъектам с сохранением состояния сохранять свое внутреннее состояние, чтобы его можно было 
восстановить, когда актор запущен, перезапущен после сбоя JVM или супервизора или перенесен в кластер. Ключевой 
концепцией упорства Акки является то, что только изменения внутреннего состояния актора сохраняются, но не всегда 
текущее состояние (за исключением факультативных снимков). Эти изменения только когда-либо добавляются к хранилищу, 
ничто никогда не мутирует, что позволяет очень высокие ставки транзакций и эффективную репликацию. Агенты-исполнители 
восстанавливаются путем повторного воспроизведения сохраненных изменений этим акторам, из которых они могут восстановить
 внутреннее состояние. Это может быть либо полная история изменений, либо начиная с моментального снимка, который может 
 значительно сократить время восстановления. Постоянство Akka также обеспечивает двухточечную связь с семантикой 
 доставки по меньшей мере один раз.

 
### Архитектура
* **`PersistentActor`**: Является постоянным актором. Он способен сохранять события в журнале и может реагировать 
на них поточно-безопасным способом. Он может использоваться для реализации как команды, так и участников, связанных с 
событиями. Когда постоянный участник запускается или перезапускается, журналированные сообщения воспроизводятся для этого
 актора, чтобы он мог восстановить внутреннее состояние из этих сообщений.
* **`AtLeastOnceDelivery`**: отправлять сообщения с семантикой доставки по крайней мере один раз в пункты назначения, а также 
в случае сбоя JVM отправителя и получателя.
* **`AsyncWriteJournal`**: журнал хранит последовательность сообщений, отправленных постоянному участнику. Приложение может 
контролировать, какие сообщения регистрируются и которые принимаются постоянным игроком, без ведения журнала.
 Журнал поддерживает наивысший уровень безопасности (`highestSequenceNr`), который увеличивается для каждого сообщения. 
 Бэкэнд хранилища журнала подключается. Расширение постоянства поставляется с плагином журнала `leveldb`, который записывается
  в локальную файловую систему. Реплицированные журналы доступны в виде [плагинов сообщества](http://akka.io/community/).
* Хранилище моментальных снимков (**`Snapshot store`**). Хранилище моментальных снимков сохраняет моментальные снимки 
внутреннего состояния постоянного актора. Снимки используются для оптимизации времени восстановления. Бэкэнд хранилища 
хранилища снимков подключается. Расширение постоянства поставляется с «локальным» плагином для хранения снимков, который 
записывается в локальную файловую систему. Реплицированные хранилища снимков доступны в виде [плагинов сообщества](http://akka.io/community/).
* Поиск событий (**`Event sourcing`**). Основываясь на строительных блоках, описанных выше, упорство Akka предоставляет 
абстракции для разработки приложений, связанных с событиями.
 
### Поиск событий (`Event sourcing`)
Постоянный актор получает (непостоянную) команду, которая сначала проверяется, если ее можно применить к текущему 
состоянию. Здесь валидация может означать что угодно: от простой проверки полей командного сообщения до беседы с 
несколькими внешними службами, например, если проверка завершается успешно, события генерируются из команды, 
представляющей эффект команды. Эти события затем сохраняются и, после успешной постоянности, используются для 
изменения состояния актора. Когда постоянный актор нуждается в восстановлении, воспроизводятся только сохраненные 
события, из которых мы знаем, что они могут быть успешно применены. Другими словами, события не могут потерпеть неудачу
 при повторном воспроизведении с постоянным игроком, в отличие от команд. Участники, участвующие в событиях, могут также
  обрабатывать команды, которые не изменяют состояние приложения, например команды запроса.

Постоянство Akka поддерживает источник событий с типажом (trait) **`PersistentActor`**. Актор, который расширяет этот типаж, 
использует **метод `persist`** для сохранения и обработки событий. Поведение `PersistentActor` определяется реализацией (переопределением)
 методов **`receiveRecover`** и **`receiveCommand`**. Это показано в следующем примере:
 
```scala
import akka.actor._
import akka.persistence._

case class Cmd(data: String)
case class Evt(data: String)

case class ExampleState(events: List[String] = Nil) {
  def updated(evt: Evt): ExampleState = copy(evt.data :: events)
  def size: Int = events.length
  override def toString: String = events.reverse.toString
}

class ExamplePersistentActor extends PersistentActor {
  override def persistenceId = "sample-id-1"

  var state = ExampleState()

  def updateState(event: Evt): Unit =
    state = state.updated(event)

  def numEvents =
    state.size

  val receiveRecover: Receive = {
    case evt: Evt                                 ⇒ updateState(evt)
    case SnapshotOffer(_, snapshot: ExampleState) ⇒ state = snapshot
  }

  val snapShotInterval = 1000
  val receiveCommand: Receive = {
    case Cmd(data) ⇒
      persist(Evt(s"${data}-${numEvents}")) { event ⇒
        updateState(event)
        context.system.eventStream.publish(event)
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(state)
      }
    case "print" ⇒ println(state)
  }

}
```

В примере определены два типа данных: **`Cmd`** и **`Evt`** для представления команд и событий соответственно. Состояние 
**`ExamplePersistentActor`** - это список сохраненных данных события, содержащихся в **`ExampleState`**.

Метод **`receiveRecover`** постоянного актора определяет, как состояние обновляется во время восстановления, обрабатывая 
сообщения `Evt` и `SnapshotOffer`. Метод **`receiveCommand` - это обработчик команд**. _В этом примере 
команда обрабатывается путем создания события, которое затем сохраняется и обрабатывается._ **События сохраняются путем 
вызова `persist` с событием (или последовательностью событий) в качестве 1-го аргумента и обработчиком события в 
качестве 2-го аргумента**.

Метод `persist` сохраняет события асинхронно, а обработчик событий выполняется для успешно сохраняемых событий. Успешно 
сохраняющиеся события внутренне отправляются обратно постоянному участнику в виде отдельных сообщений, которые вызывают 
выполнение обработчика событий. Обработчик событий может закрываться из-за состояния постоянного актора и мутировать его. 
Отправитель сохраняемого события является отправителем соответствующей команды. Это позволяет обработчикам событий 
отвечать отправителю команды (не показано).

Основная ответственность обработчика событий заключается в изменении постоянного состояния актора с использованием 
данных событий и уведомлении других об успешных изменениях состояния путем публикации событий.

При сохранении сохраняющихся событий гарантируется, что **постоянный актор не получит дальнейших команд между вызовом 
`persist` и выполнением (-ами) соответствующего обработчика события**. Это также выполняется для нескольких вызовов 
`persistent` в контексте одной команды. Входящие сообщения сохраняются до тех пор, пока сохранение не будет завершено.

Если персистентность события завершится неудачно, будет вызван **`onPersistFailure`** (_логгирование ошибки по умолчанию_), 
и актор будет безоговорочно остановлен. Если персистентность события отклоняется до его сохранения, например. из-за ошибки 
сериализации, **`onPersistRejected`** будет вызываться (_логгирование ошибки по умолчанию_), и актор продолжает 
следующее сообщение.

Самый простой способ запустить этот пример - загрузить готовый для запуска образец [Accka Persistence Sample с Scala](https://example.lightbend.com/v1/download/akka-samples-persistence-scala). 
Он содержит инструкции о том, как запустить `PersistentActorExample`. 

>Также можно переключаться между различными обработчиками команд при нормальной обработке и восстановлении с 
помощью `context.become()` и `context.unbecome()`. Чтобы заставить актора войти в одно и то же состояние после 
восстановления, вам нужно проявлять особую осторожность, чтобы выполнять одни и те же переходы состояния, становясь и 
не жертвуя в методе `receiveRecover`, как это было бы сделано в обработчике команд. Обратите внимание, что при 
использовании функции `getRecover` он будет использовать только поведение `receiveRecover` при повторном воспроизведении 
событий. Когда воспроизведение будет завершено, он будет использовать новое поведение.

#### Идентификаторы
У персистентного актора должен быть идентификатор, который не изменяется в разных воплощениях акторов. Идентификатор должен 
быть определен с помощью метода `persistenceId`.

```sbtshell
override def persistenceId = "my-stable-persistence-id"
```

>`persistenceId` должен быть уникальным для данного объекта в журнале (таблица базы данных/пространство ключей). 
Когда повторные сообщения сохраняются в журнале, вы запрашиваете сообщения с помощью `persistenceId`. Таким образом, если 
два разных объекта имеют одно и то же свойство `persistenceId`, поведение воспроизведения сообщений будет искажено.

#### Восстановление (Recovery)
По умолчанию постоянный актор автоматически восстанавливается при запуске и перезапуске путем повторной записи сообщений 
в журнале. Новые сообщения, отправленные постоянному участнику во время восстановления, не мешают воспроизведению 
сообщений. Они спрятаны и получены постоянным игроком после завершения фазы восстановления.

Количество одновременных восстановлений, которые могут выполняться одновременно, ограничено, чтобы не перегружать 
систему и хранилище данных. При превышении лимита участники будут ждать, пока не будут завершены другие выплаты. Это 
настраивается:

```properties
akka.persistence.max-concurrent-recoveries = 50
```

>Доступ к `sender()` для воспроизведенных сообщений всегда будет приводить к ссылке `deadLetters`, поскольку исходный 
отправитель считается давно ушедшим. Если вам действительно нужно уведомить актора во время восстановления в будущем, 
сохраните его `ActorPath` явно в ваших сохраненных событиях.

#### Настройка восстановления
В приложении также можно настраивать, как выполняется восстановление, возвращая настроенный объект восстановления (`Recovery`) в 
метод восстановления `PersistentActor`.

Чтобы пропустить загрузку снимков и воспроизвести все события, можно использовать `SnapshotSelectionCriteria.None`. 
Это может быть полезно, если формат сериализации моментальных снимков изменился несовместимым образом. Обычно он не 
должен использоваться, когда события были удалены.

```scala
override def recovery = Recovery(fromSnapshot = SnapshotSelectionCriteria.None)
```

Еще одна возможная настройка восстановления, которая может быть полезна для отладки, устанавливает верхнюю границу 
повтора, заставляя актора воспроизводиться только до определенной точки «в прошлом» (вместо того, чтобы переигрывать 
до самого последнего состояния ). Обратите внимание, что после этого плохой идеей является сохранение новых событий, 
потому что последующее восстановление, вероятно, будет смущено новыми событиями, которые следуют за событиями, которые 
ранее были пропущены.

```scala
override def recovery = Recovery(toSequenceNr = 457L)
```

Восстановление можно отключить, возвращая `Recovery.none()` в метод восстановления `PersistentActor`:

```scala
override def recovery = Recovery.none
```

Постоянный актор может запросить свой статус восстановления с помощью методов

```scala
def recoveryRunning: Boolean
def recoveryFinished: Boolean
```

Иногда возникает необходимость в дополнительной инициализации, когда восстановление завершилось до обработки любого 
другого сообщения, отправленного постоянному участнику. Постоянный участник получит специальное сообщение 
`RecoveryCompleted` сразу после восстановления и перед любыми другими принятыми сообщениями.

```scala
override def receiveRecover: Receive = {
  case RecoveryCompleted ⇒
  // выполнить init после восстановления, перед любыми другими сообщениями
  //...
  case evt               ⇒ //...
}

override def receiveCommand: Receive = {
  case msg ⇒ //...
}
```

Актор всегда будет получать сообщение **`RecoveryCompleted`**, даже если в журнале нет событий, а хранилище моментальных 
снимков пусто, или если это новый постоянный актор с ранее неиспользуемым `persistenceId`.

Если есть проблема с восстановлением состояния актора из журнала, вызывается `onRecoveryFailure` (регистрирует ошибку 
по умолчанию), и актор будет остановлен.

#### Внутренний stash
У персистентного актора есть личный тайник для внутреннего кэширования входящих сообщений во время восстановления или 
сохраняющихся событий `persist\persistAll`. Вы все равно можете использовать/наследовать из интерфейса `Stash`. 
`Внутренний stash` взаимодействует с обычным типом, подключаясь к методу `unstashAll` и убедившись, что сообщения были 
размыты должным образом на внутреннем тайнике, чтобы поддерживать гарантии заказа.

Вы должны быть осторожны, чтобы не отправлять больше сообщений постоянному участнику, чем он может идти в ногу с ним, 
иначе количество скрытых сообщений будет расти без ограничений. Может быть разумным защитить от `OutOfMemoryError`, указав 
максимальную пропускную способность в конфигурации почтового ящика:

```properties
akka.actor.default-mailbox.stash-capacity=10000
```

Обратите внимание, что объем закладок зависит от каждого актора. Если у вас много постоянных участников, например, 
при использовании кластерного обхода вам может потребоваться определить небольшую пропускную способность, чтобы 
гарантировать, что общее количество скрытых сообщений в системе не потребляет слишком много памяти. Кроме того, 
постоянный субъект определяет три стратегии для обработки отказа при превышении внутренней емкости. Стратегия 
переполнения по умолчанию - это `ThrowOverflowExceptionStrategy`, которая отбрасывает текущее полученное сообщение и 
генерирует исключение `StashOverflowException`, что приводит к перезапуску актора, если используется стратегия контроля по 
умолчанию. Вы можете переопределить метод innerStashOverflowStrategy, чтобы вернуть `DiscardToDeadLetterStrategy` или 
`ReplyToStrategy` для любого «индивидуального» постоянного участника или определить «по умолчанию» для всех постоянных 
участников, предоставив FQCN, который должен быть подклассом `StashOverflowStrategyConfigurator`, в конфигурации сохранения:

```properties
akka.persistence.internal-stash-overflow-strategy=
  "akka.persistence.ThrowExceptionConfigurator"
```

[Подробнее](https://doc.akka.io/docs/akka/current/persistence.html#event-sourcing)

[=> далее: Снимки (Snapshots)](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/readmes/actors/snapshots.md)


_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)