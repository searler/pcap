package pcap.streams

import java.io.File
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.io._
import pcap.codec.Codecs.WithHeaderDecoder
import pcap.data.Data
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.FileIO


/**
 * @author rsearle
 */
object Streamer extends App {
  implicit val system = ActorSystem("Sys")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()
  
 

  val f =  FileIO.fromFile(new File("/tmp/dump.pcap"))
    .transform(() => ByteStringDecoderStage(new WithHeaderDecoder))
    .collect{case data: Data if !data.bytes.isEmpty => data}
    .runForeach { println }
    .onComplete { case _ => system.shutdown }

}