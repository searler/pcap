package pcap.streams

import java.io.File
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import pcap.codec.Codecs.WithHeaderDecoder
import pcap.data.Data
import akka.stream.scaladsl.Source


/**
 * @author rsearle
 */
object Streamer extends App {
  implicit val system = ActorSystem("Sys")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val f = Source.file(new File("/tmp/dump.pcap"))
    .transform(() => ByteStringDecoderStage(new WithHeaderDecoder))
    .filter(_ match {
      case data: Data if !data.bytes.isEmpty => true
      case _                                 => false
    })
    .runForeach { println }
    .onComplete { case _ => system.shutdown }

}