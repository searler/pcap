
name := "akka-stream-pcap"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-stream-experimental" % "1.0-RC4",
  "org.scodec" %% "scodec-bits" % "1.0.9",
  "org.scodec" %% "scodec-core" % "1.8.0",
  "org.scodec" %% "scodec-protocols" % "0.10.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)



fork in run := true
