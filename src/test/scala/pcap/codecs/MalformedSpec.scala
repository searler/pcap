package pcap.codecs

import org.scalatest._
import scodec.bits.BitVector
import scodec.protocols.pcap.GlobalHeader
import pcap.codec.Codecs
import scodec.DecodeResult
import scodec.Attempt.Successful
import scodec.Decoder
import pcap.data._

/**
 * @author rsearle
 */
class MalformedSpec extends FlatSpec with Matchers {

  "malformed UDP record" should "not crash parsing" in {
    val base64 = """1MOyoQIABAAAAAAAAAAAAP//AAABAAAAPl6hVQvgDAA8AAAAPAAAAP///////wAhQwjkUwgARQAAKEPjAABAEd9lqf4Bgan+Af/a/hOIABSGXENNRAAAAAAbqf4B/wAAAAAAAD9eoVX52QYAPAAAADwAAAABgMIAAAAAH5AHOpIAJkJCAwAAAAAAkAAADlgwS+AAAAA3kAAADlgwS+CAAgIABgABAAQAc2VyYwAAAAA="""

    val bv = BitVector.fromBase64(base64).get
    println(bv)

    val full: Decoder[(Content, Content)] = for {
      gh <- GlobalHeader.codec
      rec1 <- Codecs.recordDecode(gh.ordering)
      rec2 <- Codecs.recordDecode(gh.ordering)
    } yield (rec1, rec2)

    full.decode(bv) match {
      case Successful(DecodeResult(value, remainder)) =>
        value._1.toString should be("UDP(2015-07-11T13:19:42.843-05:00,169.254.1.129,169.254.1.255,56062,5000,ByteVector(12 bytes, 0x434d44000000001ba9fe01ff))")
        value._2 should be(Unknown)
        remainder should be(BitVector.empty)
      case _@ x => fail(x.toString)
    }
  }

}