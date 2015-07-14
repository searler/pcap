package pcap.streams

import org.scalatest._
import scodec.bits.BitVector
import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import pcap.codec.Codecs.WithHeaderDecoder
import scodec.bits.ByteVector
import akka.util.ByteString
import akka.stream.scaladsl.Source
import scala.collection.immutable.Seq
import akka.stream.io.SynchronousFileSource
import java.io.File
import akka.stream.scaladsl.Sink
import akka.stream.Materializer
import scala.concurrent.ExecutionContext
import scodec.protocols.ip.Port
import scodec.codecs._
import scodec.Decoder

/**
 * @author rsearle
 */
class SingleBinaryStructuredStreamSpec extends FlatSpec with Matchers {

  val captured = ByteString(ByteVector.fromBase64(
    """1MOyoQIABAAAAAAAAAAAAP//AAABAAAAXQqlVfvBBABCAAAAQgAAAAAZ0S1zOZCxHIOdkQgARQAA
NHcpQACABlbrCgoMMgoKDGrzg2Gpjws21AAAAACAAiAAB1QAAAIEBbQBAwMIAQEEAl0KpVUuwwQA
QgAAAEIAAACQsRyDnZEAGdEtczkIAEUAADQAAEAAQAYOFQoKDGoKCgwyYanzg0yD33qPCzbVgBIW
0OR1AAACBAW0AQEEAgEDAwddCqVVX8MEADYAAAA2AAAAABnRLXM5kLEcg52RCABFAAAodypAAIAG
VvYKCgwyCgoMavODYamPCzbVTIPfe1AQAQA7GAAAXQqlVRPKBABCAAAAQgAAAAAZ0S1zOZCxHIOd
kQgARQAANHcrQACABlbpCgoMMgoKDGrzg2Gpjws21UyD33tQGAEAD3MAAAUBAAgAACWAAAgBAF0K
pVVgywQAPAAAADwAAACQsRyDnZEAGdEtczkIAEUAACiewUAAQAZvXwoKDGoKCgwyYanzg0yD33uP
CzbhUBAALjveAAAAAAAAAABdCqVVju4GADwAAAA8AAAAkLEcg52RABnRLXM5CABFAAAunsJAAEAG
b1gKCgxqCgoMMmGp84NMg997jws24VAYAC4kowAACgEAAg0qXQqlVXtJBwA6AAAAOgAAAAAZ0S1z
OZCxHIOdkQgARQAALHdBQACABlbbCgoMMgoKDGrzg2Gpjws24UyD34FQGAEAOPgAAAICAABdCqVV
50oHADwAAAA8AAAAkLEcg52RABnRLXM5CABFAAAonsNAAEAGb10KCgxqCgoMMmGp84NMg9+Bjws2
5VAQAC471AAAAAAAAAAAXQqlVT9OBwA8AAAAPAAAAJCxHIOdkQAZ0S1zOQgARQAALJ7EQABABm9Y
CgoMagoKDDJhqfODTIPfgY8LNuVQGAAuLMYAAA8CAAAAAF0KpVVOYQcAPgAAAD4AAAAAGdEtczmQ
sRyDnZEIAEUAADB3RkAAgAZW0goKDDIKCgxq84NhqY8LNuVMg9+FUBgBACyzAAAGAwAEAGQH0F0K
pVU2ZwcASwAAAEsAAACQsRyDnZEAGdEtczkIAEUAAD2exUAAQAZvRgoKDGoKCgwyYanzg0yD34WP
CzbtUBgALnxaAAABAwARZmx1c2ggcng9MTAwOjIwMDBdCqVV2LQIADoAAAA6AAAAABnRLXM5kLEc
g52RCABFAAAseNlAAIAGVUMKCgwyCgoMavODYamPCzbtTIPfmlAYAQAv0QAACwQAAF0KpVW3tggA
PAAAADwAAACQsRyDnZEAGdEtczkIAEUAACyexkAAQAZvVgoKDGoKCgwyYanzg0yD35qPCzbxUBgA
LiyfAAAPBAAAAABdCqVVr7cIADwAAAA8AAAAkLEcg52RABnRLXM5CABFAAAonsdAAEAGb1kKCgxq
CgoMMmGp84NMg9+ejws28VARAC47qgAAAAAAAAAAXQqlVcS3CAA2AAAANgAAAAAZ0S1zOZCxHIOd
kQgARQAAKHjaQACABlVGCgoMMgoKDGrzg2Gpjws28UyD359QEAEAOtgAAF0KpVVjuAgANgAAADYA
AAAAGdEtczmQsRyDnZEIAEUAACh420AAgAZVRQoKDDIKCgxq84NhqY8LNvFMg9+fUBEBADrXAABd
CqVVsrkIADwAAAA8AAAAkLEcg52RABnRLXM5CABFAAAoAABAAEAGDiEKCgxqCgoMMmGp84NMg9+f
jws28lAQAC47qQAAAAAAAAAA""").get.toByteBuffer)

  "structured" should "extract" in {
    implicit val system = ActorSystem("Sys")
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val decoder = vector(uint8 ~ uint(8) ~ variableSizeBytes(uint16, bytes))

    Source.single(captured)
      .transform(() => ByteStringDecoderStage(new WithHeaderDecoder))
      .collect { case data: pcap.data.v4.TCP => data }
      .groupBy(_.stream)
      .map {
        case (key, s) => s.runFold(ByteVector.empty) { (bv, t) => bv ++ t.bytes }
      }
      .map { _.map { bv => decoder.decodeValue(bv.bits).require } }
      .mapAsyncUnordered(1)(identity)
      .runWith(Sink.head)
      .onComplete { t =>
        system.shutdown
        val r = t.get
        r.size should be(8)
        import scodec.bits._

        r(0) should be(((5, 1), hex"0000258000080100"))
        r(1) should be(((10, 1), hex"0d2a"))
      }

    system.awaitTermination()
  }
  //Vector(((5,1),ByteVector(8 bytes, 0x0000258000080100)), ((10,1),ByteVector(2 bytes, 0x0d2a)), ((2,2),ByteVector(empty)), ((15,2),ByteVector(empty)), ((6,3),ByteVector(4 bytes, 0x006407d0)), ((1,3),ByteVector(17 bytes, 0x666c7573682072783d3130303a32303030)), ((11,4),ByteVector(empty)), ((15,4),ByteVector(empty)))

}