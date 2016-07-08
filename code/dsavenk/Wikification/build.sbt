name := "Wikification"

version := "0.1"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq(
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.6.0" classifier "models",
  "org.scala-lang.modules" % "scala-xml_2.11" % "1.0.5",
  "org.apache.commons" % "commons-compress" % "1.12",
  "org.scala-lang.modules" %% "scala-pickling" % "0.10.1"
)