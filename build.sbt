name := "Akka"

version := "0.1"

scalaVersion := "2.12.6"
val akkaVersion = "2.5.16"

libraryDependencies ++= Seq(
  "org.scalatest" %% "scalatest" % "3.2.0-SNAP10" % Test,
  "org.scalacheck" %% "scalacheck" % "1.13.5" % "test",
  "junit" % "junit" % "4.12" % Test,
  "org.junit.jupiter" % "junit-jupiter-api" % "5.2.0" % Test,
  "org.junit.jupiter" % "junit-jupiter-engine" % "5.2.0" % Test,
  "org.junit.jupiter" % "junit-jupiter-params" % "5.2.0" % Test,
  "org.junit.platform" % "junit-platform-launcher" % "1.2.0" % Test,
  "org.junit.platform" % "junit-platform-engine" % "1.2.0" % Test,
  "org.junit.platform" % "junit-platform-runner" % "1.2.0" % Test,
  "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.1",
  "com.typesafe.akka" %% "akka-actor" % akkaVersion,
  "com.typesafe.akka" %% "akka-testkit" % akkaVersion,
  "com.typesafe.akka" %% "akka-slf4j" % akkaVersion
)

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

parallelExecution in Test := false

resolvers ++= Seq(
  DefaultMavenRepository,
  Resolver.mavenLocal,
  Resolver.sonatypeRepo("snapshots"),
  Classpaths.typesafeReleases
)
