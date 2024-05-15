scalaVersion := "2.13.13"

name := "dsl-apps"
version := "1.0"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

libraryDependencies ++= List(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.3.0",
    "com.orientechnologies" % "orientdb-graphdb" % "3.0.34",
    "com.orientechnologies" % "orientdb-lucene" % "3.0.34",
    "com.typesafe.akka" %% "akka-actor-typed" % "2.9.3",
    "com.typesafe.akka" %% "akka-stream" % "2.9.3",
    "com.typesafe.akka" %% "akka-http" % "10.6.3",
)
