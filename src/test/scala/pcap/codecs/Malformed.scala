package pcap.codecs

import scodec.bits.ByteVector
import scodec.bits.BitVector
import scodec.protocols.pcap.GlobalHeader
import scodec.protocols.pcap.EthernetFrameHeader
import scodec.protocols.pcap.RecordHeader
import scodec.protocols.ip.udp.Datagram
import scodec.protocols.ip.v4.SimpleHeader
import scodec.Attempt.Successful
import scodec.DecodeResult
import pcap.codec.Codecs

/**
 * @author rsearle
 */
object Malformed extends App {

  val base64 = """1MOyoQIABAAAAAAAAAAAAP//AAABAAAAPl6hVQvgDAA8AAAAPAAAAP///////wAhQwjkUwgARQAA
KEPjAABAEd9lqf4Bgan+Af/a/hOIABSGXENNRAAAAAAbqf4B/wAAAAAAAD9eoVX52QYAPAAAADwA
AAABgMIAAAAAH5AHOpIAJkJCAwAAAAAAkAAADlgwS+AAAAA3kAAADlgwS+CAAgIABgABAAQAc2Vy
YwAAAAA="""

  val bv = BitVector.fromBase64(base64).get

  println(bv.bytes)

  val full = for {
    gh <- GlobalHeader.codec
    rec1 <- Codecs.recordDecode(gh.ordering)
    rec2 <- Codecs.recordDecode(gh.ordering)
  } yield (rec1, rec2)

  full.decode(bv) match {
    case Successful(DecodeResult(value, remainder)) =>
      println(value._1)
      println(value._2)
    case _@ x => println(x)
  }

}