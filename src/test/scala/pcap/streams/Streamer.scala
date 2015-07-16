package pcap.streams

import java.io.File
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.io.SynchronousFileSource
import pcap.codec.Codecs.WithHeaderDecoder
import pcap.data.Data
import scodec.bits.ByteVector
import akka.stream.scaladsl.Sink

/**
 * @author rsearle
 */
object Streamer extends App {
  implicit val system = ActorSystem("Sys")
  import system.dispatcher
  implicit val materializer = ActorMaterializer()

  val s =SynchronousFileSource(new File("/tmp/simple.pcap"))
  val f = s
  .map(x => {println(x);x})
    .transform(() => ByteStringDecoderStage(new WithHeaderDecoder))
    .filter(_ match {
      case data: pcap.data.v6.TCP /*if !data.bytes.isEmpty*/ => true
      case _ => false
   })
    .map { _.asInstanceOf[pcap.data.v6.TCP] }
    .groupBy(_.stream)
    .map {
      case (key, s) => (key, s.runFold(ByteVector.empty) { (bv, t) => bv ++ t.bytes })
    }
   
  //  .to(Sink.ignore).run()
   // .runForeach { r =>  r._2.map { bv => println(r._1, bv.decodeUtf8.fold(_.toString,_.toString)) } }
 .runForeach {println}
    .onComplete { case _ => system.shutdown }
    
   

}