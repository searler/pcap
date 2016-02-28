package pcap.streams

import scodec.Decoder

import scodec.bits.BitVector
import akka.util.ByteString
import akka.stream.stage.SyncDirective
import akka.stream.stage.Context
import scala.annotation.tailrec
import scodec.Attempt.Successful
import scodec.DecodeResult
import scodec.bits.ByteVector
import akka.stream.stage.PushPullStage
import akka.stream.stage.TerminationDirective

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
    new PushPullStage[T, R] {
      private var bitBuffer = BitVector.empty
      private var elements = Vector[R]()

      override def onPush(chunk: T, ctx: Context[R]): SyncDirective = {
        if (elements.isEmpty) {
          toBitVectors(chunk).foreach(bb => bitBuffer = bitBuffer ++ bb)
          elements = doParsing(elements)
        }
        onPull(ctx)
      }

      override def onPull(ctx: Context[R]): SyncDirective =
        if(ctx.isFinishing)
           elements match {
            case head +: tail =>
              elements = tail
              if(tail.isEmpty)ctx.pushAndFinish(head)else ctx.push(head)
            case _ => ctx.finish
          }else
          elements match {
            case head +: tail =>
              elements = tail
              ctx.push(head)
            case _ => ctx.pull
          }
      
       override def onUpstreamFinish(ctx: Context[R]): TerminationDirective =
      ctx.absorbTermination()
  

      @tailrec
      private def doParsing(parsedSoFar: Vector[R]): Vector[R] =
        decoder.decode(bitBuffer) match {
          case Successful(DecodeResult(value, remainder)) =>
            bitBuffer = remainder
            doParsing(parsedSoFar :+ value)
          case _ => parsedSoFar
        }

    }

  def toBitVectors(chunk: T): Iterable[BitVector]
}