package pcap.streams

import org.scalatest._
import scodec.codecs._
import akka.stream.testkit.scaladsl._
import scodec.bits.ByteVector
import akka.stream.scaladsl._
import akka.actor._
import akka.stream._
import scodec.bits._

class DecoderStageSpec extends FlatSpec with Matchers {

  val decoder = uint8 ~ uint16

  "DecoderStage" should "all at once" in {
    implicit val system = ActorSystem("Sys")
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val (pub, sub) = TestSource.probe[ByteVector]
      .transform(() => ByteVectorDecoderStage(decoder))
      .toMat(TestSink.probe[(Int, Int)])(Keep.both)
      .run()

    sub.request(n = 1)
    pub.sendNext(hex"010003")
    sub.expectNext((1, 3))

    system.shutdown
  }

  "DecoderStage" should "accept individual bytes" in {
    import scala.concurrent.duration._
    implicit val system = ActorSystem("Sys")
    import system.dispatcher
    implicit val materializer = ActorMaterializer()

    val (pub, sub) = TestSource.probe[ByteVector]
      .transform(() => ByteVectorDecoderStage(decoder))
      .toMat(TestSink.probe[(Int, Int)])(Keep.both)
      .run()

    sub.request(n = 3)
    pub.sendNext(hex"01")
    sub.expectNoMsg(10 milliseconds)
    pub.sendNext(hex"00")
    pub.sendNext(hex"03")
    sub.expectNext((1, 3))

    system.shutdown
  }
}