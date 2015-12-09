
name := "akka-stream-pcap"

version := "0.1"

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
 "com.typesafe.akka" % "akka-stream-experimental_2.11" % "2.0-M2",
  "org.scodec" %% "scodec-bits" % "1.0.11",
  "org.scodec" %% "scodec-core" % "1.8.3",
  "org.scodec" %% "scodec-protocols" % "0.10.0",
  "org.scalatest" %% "scalatest" % "2.2.4" % "test"
)



fork in run := true
