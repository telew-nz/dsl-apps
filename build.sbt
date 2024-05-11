scalaVersion := "2.13.12"

name := "dsl-apps"
version := "1.0"

libraryDependencies ++= List(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
    "com.orientechnologies" % "orientdb-graphdb" % "3.0.34",
    "com.orientechnologies" % "orientdb-lucene" % "3.0.34"
)
