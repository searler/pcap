
name := "akka-stream-pcap"

version := "0.2"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
 "com.typesafe.akka" %% "akka-stream-experimental" % "2.0.3",
 "com.typesafe.akka" %% "akka-stream-testkit-experimental" % "2.0.3",
  "org.scodec" %% "scodec-bits" % "1.0.11",
  "org.scodec" %% "scodec-core" % "1.8.3",
  "org.scodec" %% "scodec-protocols" % "0.10.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)

scalacOptions += "-deprecation"

fork in run := true
