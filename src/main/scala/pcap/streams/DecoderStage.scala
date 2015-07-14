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

/**
 * @author rsearle
 */
object DecoderStage {
  def apply[R](decoder: Decoder[R]) =
    new StatefulStage[ByteString, R] {
      private var bitBuffer = BitVector.empty

      def initial = new State {
        override def onPush(chunk: ByteString, ctx: Context[R]): SyncDirective = {
          chunk.asByteBuffers.foreach(bb => bitBuffer = bitBuffer ++ BitVector(bb))
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
}