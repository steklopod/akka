## Часть 4: Работа с группами устройств

Добавьте в свой проект следующую зависимость:

```sbtshell
    libraryDependencies += "com.typesafe.akka" %% "akka-actor" % "2.5.17"
```

### Введение

Давайте подробнее рассмотрим основные функции, необходимые для нашего использования. В полной системе IoT для контроля 
температуры дома, шаги для подключения датчика устройства к нашей системе могут выглядеть так:

1. _Датчик в доме подключается через какой-либо протокол;_
2. _Соединение с компонентом, управляющее сетевыми соединениями, принимает соединение;_
3. Датчик предоставляет идентификатор группы и устройства для регистрации в компоненте диспетчера устройств нашей системы;
4. Компонент диспетчера устройств обрабатывает регистрацию, просматривая или создавая актора, ответственного за сохранение
 состояния датчика;
5. Актор отвечает подтверждением, раскрывая его `ActorRef`;
6. Сетевой компонент теперь использует `ActorRef` для связи между датчиком и актором устройства, не проходя через диспетчер устройств.

Шаги 1 и 2 проходят за пределами нашей учебной системы. В этой главе мы рассмотрим шаги 3-6 и создадим способ регистрации 
датчиков в нашей системе и для общения с участниками. Но во-первых, у нас есть еще одно архитектурное решение - сколько 
уровней участников мы должны использовать для представления групп устройств и датчиков устройств?

Одной из основных проблем дизайна для программистов Akka является выбор лучшей детализации для акторов. На практике, 
в зависимости от характористик взаимодействия между участниками, обычно существует несколько правильных способов организации 
системы. Например, в нашем прецеденте возможно, чтобы один актор поддерживал все группы и устройства - возможно, используя 
хэш-отображения. Было бы также разумно иметь актора для каждой группы, которая отслеживает состояние всех устройств в одном доме.

Следующие рекомендации помогут нам выбрать наиболее подходящую иерархию акторов:

* **Более крупная детализация предпочтительна**. Внедрение более мелких участников вызывает больше проблем, чем решает.
* Добавьте более тонкую детализацию, когда система требует:
   * Высший параллелизм.
   * Комплексные взаимодействия между акторами, которые имеют многжество состояний. Мы увидим очень хороший пример этого 
   в следующей главе.
   * Достаточное состояние, которое имеет смысл разделить на более мелких акторов.
   * Множество несвязанных обязанностей. Использование отдельных участников позволяет людям терпеть неудачу и 
   восстанавливаться, оказывая небольшое влияние на других.

### Иерархия диспетчера устройств

Учитывая принципы, изложенные в предыдущем разделе, мы будем моделировать компонент диспетчера устройств как дерево акторов 
с тремя уровнями:

* **Актор верхнего уровня** представляет собой системный компонент для устройств. Это также точка входа для поиска и создания 
 группы устройств и акторов устройств.
* **На следующем уровне** участники группы контролируют участников устройства для одного идентификатора группы (например,
 одного дома). Они также предоставляют услуги, такие как запрос показаний температуры от всех доступных устройств в их группе.
* Агенты устройств управляют всеми взаимодействиями с фактическими датчиками устройства, такими как хранение показаний температуры.

![alt text](https://github.com/steklopod/akka/blob/akka_starter/src/main/resources/images/device_manager_tree.png "device_manager_tree")

Мы выбрали эту трехслойную архитектуру по следующим причинам:

* Наличие групп отдельных участников:
   * Изолирует сбои, которые происходят в группе. Если одному участнику удалось управлять всеми группами устройств, ошибка 
   в одной группе, которая вызывает перезагрузку, приведет к уничтожению состояния групп, которые в противном случае не 
   являются неисправными;
   * Упрощает проблему запроса всех устройств, принадлежащих к группе. Каждый участник группы содержит только состояние, 
   связанное с его группой;
   * Увеличивает параллелизм в системе. Поскольку у каждой группы есть выделенный актор, они выполняются одновременно, 
   и мы можем одновременно запрашивать несколько групп.
   
* Имея датчики, моделируемые как индивидуальные устройства:
   * Изолирует сбои одного устройства с остальными устройствами в группе;
   * Увеличивает параллельность считывания показаний температуры. Сетевые соединения с разных датчиков напрямую взаимодействуют
   с их отдельными игроками, что уменьшает количество конкурентов.

С определенной архитектурой мы можем начать работу над протоколом регистрации датчиков.

### Протокол регистрации

В качестве первого шага нам необходимо разработать протокол как для регистрации устройства, так и для создания участников 
группы и устройства, которые будут отвечать за это. Этот протокол будет предоставлен самим компонентом `DeviceManager`, 
потому что это единственный действующий субъект, который известен и доступен спереди: группы устройств и устройства 
создаются по требованию.

Глядя на регистрацию более подробно, мы можем выделить необходимые функциональные возможности:

1. Когда DeviceManager получает запрос с идентификатором группы и устройства:
   являются неисправными;
   * Если у менеджера уже есть актор для группы устройств, он отправляет ему запрос.
   являются неисправными;
   * В противном случае он создает нового участника группы устройств, а затем перенаправляет запрос.
2. Актор `DeviceGroup` получает запрос на регистрацию актора для данного устройства:
   являются неисправными;
   * Если у группы уже есть актор для устройства, участник группы отправляет запрос игроку устройства.
   являются неисправными;
   * В противном случае актор `DeviceGroup` сначала создает актора устройства, а затем перенаправляет запрос.
3. Актор устройства получает запрос и отправляет подтверждение первому отправителю. Поскольку актор устройства 
подтверждает получение (вместо группового актора), у датчика теперь будет ActorRef для отправки сообщений непосредственно 
своему актору.

Сообщения, которые мы будем использовать для регистрации запросов на регистрацию и их подтверждения, имеют простое определение:

```scala
    final case class RequestTrackDevice(groupId: String, deviceId: String)
    case object DeviceRegistered
```

В этом случае мы не включили в сообщения поле идентификатора запроса. Поскольку регистрация происходит один раз, когда 
компонент подключает систему к определенному сетевому протоколу, идентификатор не имеет значения. Однако обычно 
рекомендуется включать идентификатор запроса.

Теперь мы начнем реализацию протокола снизу вверх. На практике подход «сверху вниз» и «снизу вверх» может работать, но в 
нашем случае мы извлекаем выгоду из подхода «снизу вверх», поскольку он позволяет нам немедленно писать тесты для новых 
функций без издевательства над деталями, которые нам понадобятся для строить позже.

### Добавление поддержки регистрации участникам устройства

В нижней части нашей иерархии находятся элементы устройства. Их работа в процессе регистрации проста: ответать на запрос
 регистрации с подтверждением отправителю. Также разумно добавить защиту от запросов, которые содержат несогласованную 
 группу или идентификатор устройства.

Предположим, что идентификатор отправителя регистрационного сообщения сохраняется в верхних слоях. В следующем разделе 
мы покажем, как это может быть достигнуто.

Код регистрации актора устройства выглядит следующим образом. Измените свой пример, чтобы он соответствовал.

```scala
    object Device {
      def props(groupId: String, deviceId: String): Props = Props(new Device(groupId, deviceId))
    
      final case class RecordTemperature(requestId: Long, value: Double)
      final case class TemperatureRecorded(requestId: Long)
      final case class ReadTemperature(requestId: Long)
      final case class RespondTemperature(requestId: Long, value: Option[Double])
    }
    
    class Device(groupId: String, deviceId: String) extends Actor with ActorLogging {
      import Device._
    
      var lastTemperatureReading: Option[Double] = None
    
      override def preStart(): Unit = log.info("Device actor {}-{} started", groupId, deviceId)
      override def postStop(): Unit = log.info("Device actor {}-{} stopped", groupId, deviceId)
    
      override def receive: Receive = {
        case DeviceManager.RequestTrackDevice(`groupId`, `deviceId`) ⇒
          sender() ! DeviceManager.DeviceRegistered
    
        case DeviceManager.RequestTrackDevice(groupId, deviceId) ⇒
          log.warning(
            "Ignoring TrackDevice request for {}-{}.This actor is responsible for {}-{}.",
            groupId, deviceId, this.groupId, this.deviceId
          )
    
        case RecordTemperature(id, value) ⇒
          log.info("Recorded temperature reading {} with {}", value, id)
          lastTemperatureReading = Some(value)
          sender() ! TemperatureRecorded(id)
    
        case ReadTemperature(id) ⇒
          sender() ! RespondTemperature(id, lastTemperatureReading)
      }
    }
```

>Мы использовали функцию сопоставления шаблонов scala, где мы можем проверить, соответствует ли определенное поле 
ожидаемому значению. С помощью брекетинга переменных с обратными циклами, например `variable`, шаблон будет соответствовать 
только в том случае, если он содержит значение переменной в этой позиции.

Теперь мы можем написать два новых тестовых примера, один из которых - успешная регистрация, а другой - проверку, 
когда идентификаторы не совпадают:

```scala
    "reply to registration requests" in {
      val probe = TestProbe()
      val deviceActor = system.actorOf(Device.props("group", "device"))
    
      deviceActor.tell(DeviceManager.RequestTrackDevice("group", "device"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
      probe.lastSender should ===(deviceActor)
    }
    
    "ignore wrong registration requests" in {
      val probe = TestProbe()
      val deviceActor = system.actorOf(Device.props("group", "device"))
    
      deviceActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "device"), probe.ref)
      probe.expectNoMsg(500.milliseconds)
    
      deviceActor.tell(DeviceManager.RequestTrackDevice("group", "Wrongdevice"), probe.ref)
      probe.expectNoMsg(500.milliseconds)
    }
```

>Мы использовали вспомогательный метод `expectNoMsg()` из `TestProbe`. Это утверждение ожидает до определенного предела 
времени и терпит неудачу, если оно получает какие-либо сообщения в течение этого периода. Если в течение периода ожидания 
не принимаются сообщения, это утверждение проходит. Обычно рекомендуется сохранять эти таймауты низкими (но не слишком низкими),
 потому что они добавляют значительное время выполнения теста.
 
### Добавление поддержки регистрации участникам группы устройств

Мы закончили с поддержкой регистрации на уровне устройства, теперь мы должны реализовать его на уровне группы. У участника 
группы больше работы, когда дело касается регистрации, в том числе:

* Обработка запроса на регистрацию путем пересылки его существующему игроку устройства или путем создания нового участника и пересылки сообщения.
* Отслеживание того, какие субъекты устройства существуют в группе и удаление их из группы, когда они остановлены.

#### Обработка запроса на регистрацию

Актор группы устройств должен либо переслать запрос существующему ребенку, либо создать его. Чтобы найти дочерних субъектов
 по идентификаторам своих устройств, мы будем использовать `Map[String, ActorRef]`.

Мы также хотим сохранить идентификатор оригинального отправителя запроса, чтобы наш актор устройства мог напрямую ответить.
 Это возможно, используя `forward` вместо `!` оператор. Единственное различие между ними заключается в том, что `forward` хранит 
 оригинального отправителя `!` устанавливает отправителя как текущего участника. Так же, как с нашим игроком на устройстве, 
 мы гарантируем, что мы не будем реагировать на неправильные идентификаторы групп. Добавьте в исходный файл следующее:
 
```scala
 object DeviceGroup {
   def props(groupId: String): Props = Props(new DeviceGroup(groupId))
 }
 
 class DeviceGroup(groupId: String) extends Actor with ActorLogging {
   var deviceIdToActor = Map.empty[String, ActorRef]
 
   override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)
   override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)
 
   override def receive: Receive = {
     case trackMsg @ RequestTrackDevice(`groupId`, _) ⇒
       deviceIdToActor.get(trackMsg.deviceId) match {
         case Some(deviceActor) ⇒
           deviceActor forward trackMsg
         case None ⇒
           log.info("Creating device actor for {}", trackMsg.deviceId)
           val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")
           deviceIdToActor += trackMsg.deviceId -> deviceActor
           deviceActor forward trackMsg
       }
 
     case RequestTrackDevice(groupId, deviceId) ⇒
       log.warning(
         "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
         groupId, this.groupId
       )
   }
 }
```

Так же, как мы это сделали с устройством, мы тестируем эту новую функциональность. Мы также проверяем, что акторы, 
возвращенные для двух разных идентификаторов, фактически различны, и мы также пытаемся записать показания температуры для 
каждого из устройств, чтобы увидеть, реагируют ли участники.

```scala
    "be able to register a device actor" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))
    
      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
      val deviceActor1 = probe.lastSender
    
      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
      val deviceActor2 = probe.lastSender
      deviceActor1 should !==(deviceActor2)
    
      // Check that the device actors are working
      deviceActor1.tell(Device.RecordTemperature(requestId = 0, 1.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(requestId = 0))
      deviceActor2.tell(Device.RecordTemperature(requestId = 1, 2.0), probe.ref)
      probe.expectMsg(Device.TemperatureRecorded(requestId = 1))
    }
    
    "ignore requests for wrong groupId" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))
    
      groupActor.tell(DeviceManager.RequestTrackDevice("wrongGroup", "device1"), probe.ref)
      probe.expectNoMsg(500.milliseconds)
    }
```

Если для запроса на регистрацию уже существует действующий субъект, мы хотели бы использовать существующего актора вместо 
нового. Мы еще не тестировали это, поэтому нам нужно исправить это:


```scala
   "return same actor for same deviceId" in {
     val probe = TestProbe()
     val groupActor = system.actorOf(DeviceGroup.props("group"))
   
     groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
     probe.expectMsg(DeviceManager.DeviceRegistered)
     val deviceActor1 = probe.lastSender
   
     groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
     probe.expectMsg(DeviceManager.DeviceRegistered)
     val deviceActor2 = probe.lastSender
   
     deviceActor1 should ===(deviceActor2)
   } 
```

#### Отслеживание участников устройства в группе

До сих пор мы реализовали логику регистрации субъектов устройства в группе. Однако устройства приходят и уходят, поэтому 
нам понадобится способ удалить участников устройств с `Map[String, ActorRef]`. Мы будем предполагать, что при удалении 
устройства его соответствующий актор устройства останавливается. Наблюдение, как мы обсуждали ранее, обрабатывает только 
сценарии ошибок - не изящная остановка. Поэтому нам нужно уведомить родителя, когда один из участников устройства остановлен.

Akka предоставляет функцию `Death Watch`, которая позволяет актору смотреть другого актора и получать уведомление, если 
другой актор остановлен. В отличие от наблюдения, просмотр не ограничивается отношениями между родителем и ребенком, 
любой актор может наблюдать за любым другим актором, если он знает `ActorRef`. После остановки наблюдаемого актора 
наблюдатель получает сообщение `Terminated(actorRef)`, которое также содержит ссылку на наблюдаемого актора. Наблюдатель 
может либо явно обработать это сообщение, либо проиграть с помощью исключения DeathPactException. Это последнее полезно, 
если актор больше не может выполнять свои обязанности после того, как наблюдаемый актор был остановлен. В нашем случае 
группа должна продолжать функционировать после остановки одного устройства, поэтому нам нужно обработать сообщение 
`Terminated(actorRef)`.

Актор нашей группы устройств должен включать функциональность, которая:

1. Начинает просмотр новых устройств, когда они созданы.
2. Удаляет актора устройства с `Map[String, ActorRef]` - которая отображает устройства для участников устройства - когда 
уведомление указывает, что оно остановлено.

К сожалению, в завершенном сообщении содержится только `ActorRef` дочернего актора. Нам нужен идентификатор актора, 
чтобы удалить его с карты существующих устройств на сопоставления акторов устройства. Чтобы иметь возможность сделать это 
удаление, нам нужно ввести еще один плэйсхолдер, `Map[ActorRef, String]`, который позволит нам узнать идентификатор 
устройства, соответствующий данному `ActorRef`.

Добавление функциональности для идентификации актора приводит к следующему:

```scala
    class DeviceGroup(groupId: String) extends Actor with ActorLogging {
      var deviceIdToActor = Map.empty[String, ActorRef]
      var actorToDeviceId = Map.empty[ActorRef, String]
    
      override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)
      override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)
    
      override def receive: Receive = {
        case trackMsg @ RequestTrackDevice(`groupId`, _) ⇒
          deviceIdToActor.get(trackMsg.deviceId) match {
            case Some(deviceActor) ⇒
              deviceActor forward trackMsg
            case None ⇒
              log.info("Creating device actor for {}", trackMsg.deviceId)
              val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")
              context.watch(deviceActor)
              actorToDeviceId += deviceActor -> trackMsg.deviceId
              deviceIdToActor += trackMsg.deviceId -> deviceActor
              deviceActor forward trackMsg
          }
    
        case RequestTrackDevice(groupId, deviceId) ⇒
          log.warning(
            "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
            groupId, this.groupId
          )
    
        case Terminated(deviceActor) ⇒
          val deviceId = actorToDeviceId(deviceActor)
          log.info("Device actor for {} has been terminated", deviceId)
          actorToDeviceId -= deviceActor
          deviceIdToActor -= deviceId
    
      }
    }
```

До сих пор у нас нет средств для того, чтобы узнать, какие устройства отслеживает актор группы устройств, и поэтому мы 
еще не можем проверить нашу новую функциональность. Чтобы сделать его проверяемым, мы добавляем новую возможность запроса 
(сообщение `RequestDeviceList(requestId: Long)`), в котором перечислены активные идентификаторы устройств:

```scala
    object DeviceGroup {
      def props(groupId: String): Props = Props(new DeviceGroup(groupId))
    
      final case class RequestDeviceList(requestId: Long)
      final case class ReplyDeviceList(requestId: Long, ids: Set[String])
    }
    
    class DeviceGroup(groupId: String) extends Actor with ActorLogging {
      var deviceIdToActor = Map.empty[String, ActorRef]
      var actorToDeviceId = Map.empty[ActorRef, String]
    
      override def preStart(): Unit = log.info("DeviceGroup {} started", groupId)
      override def postStop(): Unit = log.info("DeviceGroup {} stopped", groupId)
    
      override def receive: Receive = {
        case trackMsg @ RequestTrackDevice(`groupId`, _) ⇒
          deviceIdToActor.get(trackMsg.deviceId) match {
            case Some(deviceActor) ⇒
              deviceActor forward trackMsg
            case None ⇒
              log.info("Creating device actor for {}", trackMsg.deviceId)
              val deviceActor = context.actorOf(Device.props(groupId, trackMsg.deviceId), s"device-${trackMsg.deviceId}")
              context.watch(deviceActor)
              actorToDeviceId += deviceActor -> trackMsg.deviceId
              deviceIdToActor += trackMsg.deviceId -> deviceActor
              deviceActor forward trackMsg
          }
    
        case RequestTrackDevice(groupId, deviceId) ⇒
          log.warning(
            "Ignoring TrackDevice request for {}. This actor is responsible for {}.",
            groupId, this.groupId
          )
    
        case RequestDeviceList(requestId) ⇒
          sender() ! ReplyDeviceList(requestId, deviceIdToActor.keySet)
    
        case Terminated(deviceActor) ⇒
          val deviceId = actorToDeviceId(deviceActor)
          log.info("Device actor for {} has been terminated", deviceId)
          actorToDeviceId -= deviceActor
          deviceIdToActor -= deviceId
    
      }
    }
```

Мы почти готовы проверить удаление устройств. Но нам все еще нужны следующие возможности:

* Чтобы остановить актора устройства из нашего тестового примера. С внешней стороны любой актор может быть остановлен, 
отправив специальное встроенное сообщение `PoisonPill`, которое инструктирует актора остановиться.
* Чтобы получать уведомление после остановки устройства. Для этого мы также можем использовать средство `Death Watch`
 для этой цели. `TestProbe` имеет два сообщения, которые мы можем легко использовать, `watch()`, чтобы наблюдать за 
 конкретным актором, и ожидать, что он будет уверен, что наблюдаемый актор был прекращен.

Теперь мы добавим еще два теста. Во-первых, мы проверяем, что мы вернем список правильных идентификаторов, как только 
мы добавим несколько устройств. Второй тестовый пример удостоверяет, что идентификатор устройства должным образом удален 
после остановки устройства:

```scala
    "be able to list active devices" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))
    
      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
    
      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
    
      groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 0), probe.ref)
      probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 0, Set("device1", "device2")))
    }
    
    "be able to list active devices after one shuts down" in {
      val probe = TestProbe()
      val groupActor = system.actorOf(DeviceGroup.props("group"))
    
      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device1"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
      val toShutDown = probe.lastSender
    
      groupActor.tell(DeviceManager.RequestTrackDevice("group", "device2"), probe.ref)
      probe.expectMsg(DeviceManager.DeviceRegistered)
    
      groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 0), probe.ref)
      probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 0, Set("device1", "device2")))
    
      probe.watch(toShutDown)
      toShutDown ! PoisonPill
      probe.expectTerminated(toShutDown)
    
      // using awaitAssert to retry because it might take longer for the groupActor
      // to see the Terminated, that order is undefined
      probe.awaitAssert {
        groupActor.tell(DeviceGroup.RequestDeviceList(requestId = 1), probe.ref)
        probe.expectMsg(DeviceGroup.ReplyDeviceList(requestId = 1, Set("device2")))
      }
    }
```

### Создание операторов диспетчера устройств

Перейдя на следующий уровень в нашей иерархии, нам нужно создать точку входа для нашего компонента менеджера устройств в 
исходном файле `DeviceManager`. Этот актор очень похож на актора группы устройств, но создает акторов группы устройств, 
а не акторов устройства:

```scala
    object DeviceManager {
      def props(): Props = Props(new DeviceManager)
    
      final case class RequestTrackDevice(groupId: String, deviceId: String)
      case object DeviceRegistered
    }
    
    class DeviceManager extends Actor with ActorLogging {
      var groupIdToActor = Map.empty[String, ActorRef]
      var actorToGroupId = Map.empty[ActorRef, String]
    
      override def preStart(): Unit = log.info("DeviceManager started")
      override def postStop(): Unit = log.info("DeviceManager stopped")
    
      override def receive = {
        case trackMsg @ RequestTrackDevice(groupId, _) ⇒
          groupIdToActor.get(groupId) match {
            case Some(ref) ⇒
              ref forward trackMsg
            case None ⇒
              log.info("Creating device group actor for {}", groupId)
              val groupActor = context.actorOf(DeviceGroup.props(groupId), "group-" + groupId)
              context.watch(groupActor)
              groupActor forward trackMsg
              groupIdToActor += groupId -> groupActor
              actorToGroupId += groupActor -> groupId
          }
    
        case Terminated(groupActor) ⇒
          val groupId = actorToGroupId(groupActor)
          log.info("Device group actor for {} has been terminated", groupId)
          actorToGroupId -= groupActor
          groupIdToActor -= groupId
    
      }
    
    }
```

Мы оставляем тесты менеджера устройств в качестве упражнения для вас, так как он очень похож на те тесты, которые мы уже 
писали для группового актора.

### Что дальше?

Теперь у нас есть иерархический компонент для регистрации и отслеживания устройств и записи измерений. Мы видели, как 
реализовать различные типы шаблонов беседы, такие как:

* `Request-respond` (для записи температуры)
* `Delegate-respond` (для регистрации устройств)
* `Create-watch-terminate` (для создания актора группы и устройства в качестве детей)

В следующей главе мы расскажем о возможностях группового запроса, который установит новый шаблон разбора рассеяния. 
В частности, мы реализуем функциональность, которая позволяет пользователям запрашивать статус всех устройств, принадлежащих 
к группе.

_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)