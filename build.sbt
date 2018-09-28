name := "Akka"

version := "0.1"

scalaVersion := "2.12.6"

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
  "com.typesafe.akka" %% "akka-actor" % "2.5.16"
)

testFrameworks += new TestFramework("org.scalameter.ScalaMeterFramework")

parallelExecution in Test := false

resolvers ++= Seq(
  Resolver.sonatypeRepo("releases"),
  Resolver.sonatypeRepo("snapshots"),
  Classpaths.typesafeReleases
)
