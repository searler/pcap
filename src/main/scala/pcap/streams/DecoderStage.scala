package pcap.streams

import scodec.Decoder
import akka.stream.stage.StatefulStage
import scodec.bits.BitVector
import akka.util.ByteString
import akka.stream.stage.SyncDirective
import akka.stream.stage.Context
import scala.annotation.tailrec
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits.ByteVector

/**
 * @author rsearle
 */
object ByteStringDecoderStage extends DecoderStage[ByteString] {
  def toBitVectors(chunk: ByteString): Iterable[BitVector] = chunk.asByteBuffers.map(BitVector.apply)
}

object BitVectorDecoderStage extends DecoderStage[BitVector] {
  def toBitVectors(chunk: BitVector): Iterable[BitVector] = Iterable(chunk)
}

object ByteVectorDecoderStage extends DecoderStage[ByteVector] {
  def toBitVectors(chunk: ByteVector): Iterable[BitVector] = Iterable(chunk.bits)
}

trait DecoderStage[T] {
  def apply[R](decoder: Decoder[R]) =
    new StatefulStage[T, R] {
      private var bitBuffer = BitVector.empty

      def initial = new State {
        override def onPush(chunk: T, ctx: Context[R]): SyncDirective = {
          toBitVectors(chunk).foreach(bb => bitBuffer = bitBuffer ++ bb)
          val elements = doParsing(Vector.empty)
          emit(elements.iterator, ctx)
        }

        @tailrec
        private def doParsing(parsedSoFar: Vector[R]): Vector[R] =
          decoder.decode(bitBuffer) match {
            case Successful(DecodeResult(value, remainder)) =>
              bitBuffer = remainder
              doParsing(parsedSoFar :+ value)
            case _ => parsedSoFar
          }
      }
    }

  def toBitVectors(chunk: T): Iterable[BitVector]
}