name := "kbqa"

version := "1.0"

scalaVersion := "2.11.7"

javacOptions ++= Seq("-source", "1.8", "-target", "1.8")

// Fixing a problem with overwritten compiler version by a dependency.
ivyScala := ivyScala.value map { _.copy(overrideScalaVersion = true) }

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
  "net.databinder.dispatch" % "dispatch-json4s-native_2.11" % "0.11.3",
  "org.json4s" % "json4s-native_2.11" % "3.2.11",
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1",
  "com.typesafe" % "config" % "1.3.0",
  "org.slf4j" % "slf4j-api" % "1.7.12",
  "org.slf4j" % "slf4j-simple" % "1.7.12",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.1.0",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)