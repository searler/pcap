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

/**
 * @author rsearle
 */
class SingleTextStreamSpec extends FlatSpec with Matchers {

  val bytes = ByteString(ByteVector.fromBase64("""1MOyoQIABAAAAAAAAAAAAAAABAABAAAAaeCiVUj7AABeAAAAXgAAAAAAAAAAAAAAAAAAAIbdYAAA
AAAoBkAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAa+HJw8erlORAAAAAKACqqoAMAAA
AgT/xAQCCAoKWRkEAAAAAAEDAwdp4KJVafsAAF4AAABeAAAAAAAAAAAAAAAAAAAAht1gAAAAACgG
QAAAAAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABJw+vh0/kggMerlOSoBKqqgAwAAACBP/E
BAIICgpZGQUKWRkEAQMDB2ngolV6+wAAVgAAAFYAAAAAAAAAAAAAAAAAAACG3WAAAAAAIAZAAAAA
AAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAGvhycPHq5Tkk/kggSAEAFWACgAAAEBCAoKWRkF
ClkZBWvgolWhLwEAXAAAAFwAAAAAAAAAAAAAAAAAAACG3WAAAAAAJgZAAAAAAAAAAAAAAAAAAAAA
AQAAAAAAAAAAAAAAAAAAAAGvhycPHq5Tkk/kggSAGAFWAC4AAAEBCAoKWSDiClkZBWhlbGxvCmvg
olXiLwEAVgAAAFYAAAAAAAAAAAAAAAAAAACG3WAAAAAAIAZAAAAAAAAAAAAAAAAAAAAAAQAAAAAA
AAAAAAAAAAAAAAEnD6+HT+SCBB6uU5iAEAFWACgAAAEBCAoKWSDiClkg4m/golUHQg0AXAAAAFwA
AAAAAAAAAAAAAAAAAACG3WAAAAAAJgZAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAEn
D6+HT+SCBB6uU5iAGAFWAC4AAAEBCAoKWTOZClkg4nRoZXJlCm/golUrQg0AVgAAAFYAAAAAAAAA
AAAAAAAAAACG3WAAAAAAIAZAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAGvhycPHq5T
mE/kggqAEAFWACgAAAEBCAoKWTOZClkzmXPgolXNUAIAWgAAAFoAAAAAAAAAAAAAAAAAAACG3WAA
AAAAJAZAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAGvhycPHq5TmE/kggqAGAFWACwA
AAEBCAoKWUBsClkzmW9uZQpz4KJVDlECAFYAAABWAAAAAAAAAAAAAAAAAAAAht1gAAAAACAGQAAA
AAAAAAAAAAAAAAAAAAEAAAAAAAAAAAAAAAAAAAABJw+vh0/kggoerlOcgBABVgAoAAABAQgKCllA
bApZQGx24KJVMy4JAFoAAABaAAAAAAAAAAAAAAAAAAAAht1gAAAAACQGQAAAAAAAAAAAAAAAAAAA
AAEAAAAAAAAAAAAAAAAAAAABJw+vh0/kggoerlOcgBgBVgAsAAABAQgKCllN5gpZQGx0d28KduCi
VVkuCQBWAAAAVgAAAAAAAAAAAAAAAAAAAIbdYAAAAAAgBkAAAAAAAAAAAAAAAAAAAAABAAAAAAAA
AAAAAAAAAAAAAa+HJw8erlOcT+SCDoAQAVYAKAAAAQEICgpZTeYKWU3meeCiVbwGCQBaAAAAWgAA                                                                         
AAAAAAAAAAAAAAAAAIbdYAAAAAAkBkAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAa+H                                                                         
Jw8erlOcT+SCDoAYAVYALAAAAQEICgpZWZQKWU3mZW5kCnngolX9BgkAVgAAAFYAAAAAAAAAAAAA                                                                         
AAAAAACG3WAAAAAAIAZAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAEnD6+HT+SCDh6u                                                                         
U6CAEAFWACgAAAEBCAoKWVmUCllZlHzgolWkSAcAWwAAAFsAAAAAAAAAAAAAAAAAAACG3WAAAAAA                                                                         
JQZAAAAAAAAAAAAAAAAAAAAAAQAAAAAAAAAAAAAAAAAAAAEnD6+HT+SCDh6uU6CAGAFWAC0AAAEB                                                                         
CAoKWWTZCllZlGRvbmUKfOCiVeBIBwBWAAAAVgAAAAAAAAAAAAAAAAAAAIbdYAAAAAAgBkAAAAAA                                                                         
AAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAa+HJw8erlOgT+SCE4AQAVYAKAAAAQEICgpZZNoK                                                                         
WWTZf+CiVS8yBwBWAAAAVgAAAAAAAAAAAAAAAAAAAIbdYAAAAAAgBkAAAAAAAAAAAAAAAAAAAAAB                                                                         
AAAAAAAAAAAAAAAAAAAAAa+HJw8erlOgT+SCE4ARAVYAKAAAAQEICgpZcIwKWWTZf+CiVYkyBwBW                                                                         
AAAAVgAAAAAAAAAAAAAAAAAAAIbdYAAAAAAgBkAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAA                                                                         
AAAAAScPr4dP5IITHq5ToYARAVYAKAAAAQEICgpZcIwKWXCMf+CiVasyBwBWAAAAVgAAAAAAAAAA                                                                         
AAAAAAAAAIbdYAAAAAAgBkAAAAAAAAAAAAAAAAAAAAABAAAAAAAAAAAAAAAAAAAAAa+HJw8erlOh
T+SCFIAQAVYAKAAAAQEICgpZcIwKWXCM""").get.toByteBuffer)

  "stream" should "extract all text in each direction" in {
    implicit val system = ActorSystem("Sys")
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val f = Source.single(bytes)
      .transform(() => DecoderStage(new WithHeaderDecoder))
      .filter(_ match {
        case data: pcap.data.v6.TCP => true
        case _                      => false
      })
      .map { _.asInstanceOf[pcap.data.v6.TCP] }
      .groupBy(_.stream)
      .map {
        case (key, s) => (key, s.runFold(ByteVector.empty) { (bv, t) => bv ++ t.bytes })
      }
      .map { r => r._2.map { bv => (r._1, bv.decodeUtf8.fold(_.toString, _.toString)) } }
      .runWith(Sink.head)
      .onComplete(t1 => t1.get.onComplete { v =>
        v.get._2 should be("hello\nthere\none\ntwo\nend\ndone\n")
        system.shutdown()
      }) 

    system.awaitTermination()
  }
}