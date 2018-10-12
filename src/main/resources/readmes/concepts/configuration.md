## Конфигурация

Вы можете начать использовать Akka без определения какой-либо конфигурации, так как предоставляются разумные значения
 по умолчанию. Позже вам может потребоваться изменить настройки, чтобы изменить поведение по умолчанию или адаптировать
  для определенных сред выполнения. Типичные примеры настроек, которые вы можете изменить:

* уровень логирования и бэкэнд
* включить удаленный доступ
* сериализаторы сообщений
* определение маршрутизаторов
* настройка диспетчеров

Akka использует библиотеку [Configafe Config](https://github.com/typesafehub/config), которая также может быть хорошим 
выбором для конфигурации вашего собственного приложения или библиотеки, построенной с помощью Akka или без нее. Эта 
библиотека реализована на Java без внешних зависимостей; вам следует взглянуть на его документацию (в частности, на [ConfigFactory](https://lightbend.github.io/config/latest/api/com/typesafe/config/ConfigFactory.html)),
 которая только обобщается ниже.

### Откуда конфигурация считывается 
Вся конфигурация для Akka проводится в экземплярах `ActorSystem` или, по-разному, с учетом внешних факторов, 
`ActorSystem` является единственным потребителем информации о конфигурации. При создании системы акторов вы можете либо
 передать объект `Config`, либо нет, где второй случай эквивалентен передаче `ConfigFactory.load()` (с правильным 
 загрузчиком классов). Это означает, что по умолчанию используется синтаксическое разбор всех файлов `application.conf`,
  `application.json` и `application.properties`, найденных в корневой папке пути. Более подробную информацию см. В 
  вышеупомянутой документации. 
  
Затем актор-система объединяется во все ресурсы `reference.conf`, найденные в корне пути класса, чтобы сформировать 
резервную конфигурацию, то есть она внутренне использует

```scala
  appConfig.withFallback(ConfigFactory.defaultReference(classLoader))
```

Философия заключается в том, что код никогда не содержит значений по умолчанию, но вместо этого полагается на их 
присутствие в `reference.conf`, поставляемом с соответствующей библиотекой.

Наивысший приоритет отдается переопределениям, заданным как свойства системы, см. [Спецификацию HOCON](https://github.com/typesafehub/config/blob/master/HOCON.md).
 Также следует отметить, что конфигурация приложения, которая по умолчанию применяется к приложению, может быть
  переопределена с использованием свойства `config.resource` (их больше, обратитесь к документам [Config](https://github.com/typesafehub/config/blob/master/README.md)).

>Если вы пишете приложение Akka, сохраните свою конфигурацию в `application.conf` в корне пути класса. 
Если вы пишете библиотеку на базе Akka, сохраните ее конфигурацию в файле `reference.conf` в корневом каталоге JAR-файла.

### Когда использовать JarJar, OneJar, Assembly или любого jar-bundler

>Принцип настройки Akka в значительной степени зависит от понятия каждого модуля/jar, имеющего свой собственный файл 
`reference.conf`, все они будут обнаружены конфигурацией и загружены. К сожалению, это также означает, что если 
вы поместите/объедините несколько jar в тот же jar, вам также необходимо объединить все `reference.confs`. В противном
 случае все значения по умолчанию будут потеряны, и Akka не будет функционировать.

Если вы используете Maven для упаковки своего приложения, вы также можете использовать поддержку 
`Apache Maven Shade Plugin` для `Resource Transformers`, чтобы объединить все `reference.confs` в пути класса сборки в один.

Конфигурация плагина может выглядеть так:

```xml
<plugin>
 <groupId>org.apache.maven.plugins</groupId>
 <artifactId>maven-shade-plugin</artifactId>
 <version>1.5</version>
 <executions>
  <execution>
   <phase>package</phase>
   <goals>
    <goal>shade</goal>
   </goals>
   <configuration>
    <shadedArtifactAttached>true</shadedArtifactAttached>
    <shadedClassifierName>allinone</shadedClassifierName>
    <artifactSet>
     <includes>
      <include>*:*</include>
     </includes>
    </artifactSet>
    <transformers>
      <transformer
       implementation="org.apache.maven.plugins.shade.resource.AppendingTransformer">
       <resource>reference.conf</resource>
      </transformer>
      <transformer
       implementation="org.apache.maven.plugins.shade.resource.ManifestResourceTransformer">
       <manifestEntries>
        <Main-Class>akka.Main</Main-Class>
       </manifestEntries>
      </transformer>
    </transformers>
   </configuration>
  </execution>
 </executions>
</plugin>
```

### Пользовательский application.conf
Пользовательский `application.conf` может выглядеть следующим образом:

```yaml
# В этом файле вы можете переопределить любую опцию, определенную в файлах ссылок.
# Скопируйте части файлов ссылок и измените их, как вам угодно.

akka {

    # Регистраторы регистрируются во время загрузки (akka.event.Logging $ DefaultLogger logs в STDOUT)
  loggers = ["akka.event.slf4j.Slf4jLogger"]

    # Уровень журнала, используемый настроенными регистраторами (см. «Регистраторы»), как только
    #   поскольку они были запущены; до этого см. «stdout-loglevel»
    #   Опции: ВЫКЛ, ОШИБКА, ПРЕДУПРЕЖДЕНИЕ, ИНФОРМАЦИЯ, DEBUG
  loglevel = "DEBUG"

    # Уровень журнала для самого основного регистратора, активированного при запуске ActorSystem.
    # Этот регистратор печатает сообщения журнала в stdout (System.out).
    #  Опции: ВЫКЛ, ОШИБКА, ПРЕДУПРЕЖДЕНИЕ, ИНФОРМАЦИЯ, DEBUG
  stdout-loglevel = "DEBUG"

    # Фильтр событий журнала, который используется LoggingAdapter до
    # публикация событий журнала в eventStream.
  logging-filter = "akka.event.slf4j.Slf4jLoggingFilter"

  actor {
    provider = "cluster"

    default-dispatcher {
      # Пропускная способность для диспетчера по умолчанию, установленная на 1 для максимально возможной
      throughput = 10
    }
  }

  remote {
    # К портам следует подключиться. Значение по умолчанию - 2552.
    netty.tcp.port = 4711
  }
}
```

### Добавление файлов
Иногда бывает полезно включить другой файл конфигурации, например, если у вас есть один `application.conf` со всеми 
независимыми от среды настройками, а затем переопределить некоторые настройки для определенных сред.

Указание системного свойства с `-Dconfig.resource=/ dev.conf` будет загружать файл `dev.conf`, который включает 
`application.conf`

>dev.conf

```yaml
include "application"

akka {
  loglevel = "DEBUG"
}
```


### Логирование конфигурации
Если системное или конфигурационное поле `config akka.log-config-on-start` установлено значение `on`, тогда полная 
конфигурация регистрируется на уровне INFO при запуске системы. Это полезно, если вы не уверены в том, какая конфигурация
 используется.

Если вы сомневаетесь, вы можете проверить свои объекты конфигурации до или после их использования для создания системы актеров:

```scala
scala> import com.typesafe.config._
import com.typesafe.config._

scala> ConfigFactory.parseString("a.b=12")
res0: com.typesafe.config.Config = Config(SimpleConfigObject({"a" : {"b" : 12}}))

scala> res0.root.render
res1: java.lang.String =
{
    # String: 1
    "a" : {
        # String: 1
        "b" : 12
    }
}
```

Комментарии, предшествующие каждому элементу, содержат подробную информацию о происхождении установки (номер файла и 
строки) плюс возможные комментарии, которые присутствовали, например. в эталонной конфигурации. Параметры, 
объединенные со ссылкой и проанализированные системой актеров, могут отображаться следующим образом:
```scala
final ActorSystem system = ActorSystem.create();
System.out.println(system.settings());
// это короткая запись для system.settings().config().root().render()
```

### Слово о ClassLoaders
В нескольких местах конфигурационного файла можно указать полностью квалифицированное имя класса, которое будет 
создано Akka. Это делается с использованием отражения Java, которое, в свою очередь, использует ClassLoader. 
Получение правильного решения в сложных средах, таких как контейнеры приложений или пакеты OSGi, не всегда тривиально, 
текущий подход Akka заключается в том, что каждая реализация ActorSystem хранит загрузчик контекстного класса текущего 
потока (если он доступен, в противном случае это просто его собственный загрузчик, как в this.getClass. getClassLoader) и 
использует это для всех рефлексивных доступов. Это означает, что включение Akka в путь класса загрузки приведет к 
исключению NullPointerException из странных мест: это не поддерживается.

### Специальные настройки приложения
Конфигурация также может использоваться для конкретных приложений. Хорошей практикой является размещение этих настроек
 в [расширении (Extension)](https://doc.akka.io/docs/akka/current/extending-akka.html#extending-akka-settings).

###Настройка нескольких ActorSystem
Если у вас есть несколько ActorSystem (или вы пишете библиотеку и имеете ActorSystem, которые могут быть 
отделены от приложения), вы можете захотеть разделить конфигурацию для каждой системы.

Учитывая, что `ConfigFactory.load()` объединяет все ресурсы с соответствующим именем из всего пути класса, проще
 всего использовать эту функциональность и дифференцировать актерские системы в иерархии конфигурации:
 
 ```yaml
myapp1 {
  akka.loglevel = "WARNING"
  my.own.setting = 43
}
myapp2 {
  akka.loglevel = "ERROR"
  app2.setting = "appname"
}
my.own.setting = 42
my.other.setting = "hello"
```

```scala
val config = ConfigFactory.load()
val app1 = ActorSystem("MyApp1", config.getConfig("myapp1").withFallback(config))
val app2 = ActorSystem("MyApp2",
  config.getConfig("myapp2").withOnlyPath("akka").withFallback(config))
```

Эти два примера демонстрируют различные варианты трюка «лифт-а-поддерево»: в первом случае конфигурация, доступная 
изнутри актерской системы, - это

```properties
akka.loglevel = "WARNING"
my.own.setting = 43
my.other.setting = "hello"
# плюс myapp1 и myapp2 поддеревья
```
в то время как во втором, только поддерево «akka» снимается со следующим результатом

```properties
akka.loglevel = "ERROR"
my.own.setting = 42
my.other.setting = "hello"
# плюс myapp1 и myapp2 поддеревья
```

Конфигурационная библиотека действительно мощная, объясняя, что все функции превосходят доступную здесь область. 
В частности, не рассматривается, как включить другие файлы конфигурации в другие файлы (см. Небольшой пример 
при [включении файлов](https://doc.akka.io/docs/akka/current/general/configuration.html#including-files)) и 
копировать части дерева конфигурации посредством замещения пути.

Вы можете также указать и проанализировать конфигурацию программным способом другими способами при создании 
экземпляра ActorSystem.

```scala
import akka.actor.ActorSystem
import com.typesafe.config.ConfigFactory
val customConf = ConfigFactory.parseString("""
  akka.actor.deployment {
    /my-service {
      router = round-robin-pool
      nr-of-instances = 3
    }
  }
  """)
// ConfigFactory.load sandwiches customConfig between default reference
// config and default overrides, and then resolves it.
val system = ActorSystem("MySystem", ConfigFactory.load(customConf))
```

* [полное описание всех настроек](https://doc.akka.io/docs/akka/current/general/configuration.html)

_Если этот проект окажется полезным тебе - нажми на кнопочку **`★`** в правом верхнем углу._

[<= содержание](https://github.com/steklopod/akka/blob/akka_starter/readme.md)