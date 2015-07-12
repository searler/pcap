package pcap.streams

import java.io.File
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.io.SynchronousFileSource
import pcap.codec.Codecs.Wrapper
import pcap.data.Data


/**
 * @author rsearle
 */
object Streamer extends App {
  implicit val system = ActorSystem("Sys")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val f = SynchronousFileSource(new File("/tmp/dump.pcap"))
    .transform(() => DecoderStage(new Wrapper))
    .filter(_ match {
      case data: Data if !data.bytes.isEmpty => true
      case _                                 => false
    })
    .runForeach { println }
    .onComplete { case _ => system.shutdown }

}