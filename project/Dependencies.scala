import sbt._

object Dependencies {

  val coreDependencies = Seq(
    "org.scalatest" %% "scalatest" % "3.3.0-SNAP3" % "test",
    "org.scalatestplus" %% "mockito-3-4" % "3.2.10.0" % Test,
    "com.typesafe.akka" %% "akka-actor-testkit-typed" % "2.6.19" % Test,
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.4",
    "org.slf4j" % "slf4j-api" % "1.7.36",
    "org.slf4j" % "slf4j-log4j12" % "1.7.36",
    "com.typesafe.akka" %% "akka-actor" % "2.6.19",
    "com.typesafe.akka" %% "akka-stream" % "2.6.19",
    "com.typesafe.akka" %% "akka-stream-typed" % "2.6.19",
    "com.typesafe.akka" %% "akka-http" % "10.2.9",
    "com.typesafe.akka" %% "akka-slf4j" % "2.6.19",
    "com.typesafe.akka" %% "akka-actor-typed" % "2.6.19",
    "org.apache.commons" % "commons-lang3" % "3.12.0",
    "org.scala-lang.modules" %% "scala-xml" % "1.3.0",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2",
    "javax.xml.bind" % "jaxb-api" % "2.3.1",
    "com.softwaremill.retry" %% "retry" % "0.3.4",
    "org.bouncycastle" % "bcprov-jdk15on" % "1.70",
    "com.github.javafaker" % "javafaker" % "1.0.2" % "test",
    "org.scala-lang.modules" %% "scala-parallel-collections" % "1.0.4",
    "joda-time" % "joda-time" % "2.10.14",
    "com.typesafe.play" %% "play-json" % "2.9.2",
    ("com.github.mifmif" % "generex" % "1.0.2")
      .exclude("junit", "junit")
  )

}
