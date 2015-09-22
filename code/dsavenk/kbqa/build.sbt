name := "kbqa"

version := "1.0"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "net.databinder.dispatch" %% "dispatch-core" % "0.11.3",
  "net.databinder.dispatch" % "dispatch-json4s-native_2.11" % "0.11.3",
  "org.json4s" % "json4s-native_2.11" % "3.2.11",
  "org.scalatest" % "scalatest_2.11" % "2.2.4" % "test"
)