scalaVersion := "2.13.14"

name := "dsl-apps"
version := "1.0"
maintainer := "secret"

resolvers += "Akka library repository".at("https://repo.akka.io/maven")

libraryDependencies ++= List(
    "org.scala-lang.modules" %% "scala-parser-combinators" % "2.4.0",
    "com.orientechnologies" % "orientdb-graphdb" % "3.0.34",
    "com.orientechnologies" % "orientdb-lucene" % "3.0.34",
    "com.typesafe.akka" %% "akka-actor-typed" % "2.9.3",
    "com.typesafe.akka" %% "akka-stream" % "2.9.3",
    "com.typesafe.akka" %% "akka-http" % "10.6.3",
    "com.github.pureconfig" %% "pureconfig" % "0.17.7",
    "org.slf4j" % "slf4j-nop" % "2.0.13",
)

enablePlugins(JavaAppPackaging)
