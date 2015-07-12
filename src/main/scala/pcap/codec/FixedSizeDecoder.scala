package pcap.codec

import scodec.Decoder
import scodec.bits.BitVector
import scodec.Err
import scodec.Attempt
import scodec.DecodeResult

/**
 * @author rsearle
 */
class FixedSizeDecoder[A](size: Long, decoder: Decoder[A]) extends Decoder[A] {

  override def decode(buffer: BitVector): Attempt[DecodeResult[A]] = {
    if (buffer.sizeGreaterThanOrEqual(size)) {
      decoder.decode(buffer.take(size)) map { res =>
        DecodeResult(res.value, buffer.drop(size))
      }
    } else {
      Attempt.failure(Err.insufficientBits(size, buffer.size))
    }
  }
}