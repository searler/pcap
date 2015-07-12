package pcap.codec

import org.joda.time.DateTime

import pcap._

import scodec.Decoder
import scodec.bits.ByteOrdering

import scodec.codecs.bits
import scodec.codecs.provide
import scodec.protocols.ip.Protocols
import scodec.protocols.ip.tcp.TcpHeader
import scodec.protocols.ip.udp.Datagram
import scodec.protocols.ip.v4
import scodec.protocols.ip.v6
import scodec.protocols.pcap.EtherType
import scodec.protocols.pcap.EthernetFrameHeader
import scodec.protocols.pcap.GlobalHeader
import scodec.protocols.pcap.RecordHeader

/**
 * @author rsearle
 */
object Codecs {

  def v4TcpDecode(timestamp: DateTime, sh: v4.SimpleHeader) =
    for {
      th <- TcpHeader.codec
      bv <- bits((sh.dataLength - th.dataOffset * 4) * 8)
    } yield data.v4.TCP(timestamp, sh.sourceIp, sh.destinationIp,
      th.sourcePort, th.destinationPort, th.sequenceNumber, bv.bytes)

  def v4UdpDecode(timestamp: DateTime, sh: v4.SimpleHeader) =
    for { dg <- Datagram.codec }
      yield data.v4.UDP(timestamp, sh.sourceIp, sh.destinationIp, dg.sourcePort, dg.destinationPort, dg.data.bytes)

  def v4DataDecode(rh: RecordHeader) = {
    def parse(sh: v4.SimpleHeader) = sh.protocol match {
      case Protocols.Tcp => v4TcpDecode(rh.timestamp, sh)
      case Protocols.Udp => v4UdpDecode(rh.timestamp, sh)
      case _             => provide(data.Unknown)
    }
    for {
      sh <- v4.SimpleHeader.codec
      p <- parse(sh)
    } yield p
  }

  def v6TcpDecode(timestamp: DateTime, sh: v6.SimpleHeader) =
    for {
      th <- TcpHeader.codec
      bv <- bits((sh.payloadLength - th.dataOffset * 4) * 8)
    } yield data.v6.TCP(timestamp, sh.sourceIp, sh.destinationIp,
      th.sourcePort, th.destinationPort, th.sequenceNumber, bv.bytes)

  def v6UdpDecode(timestamp: DateTime, sh: v6.SimpleHeader) = for {
    dg <- Datagram.codec
  } yield data.v6.UDP(timestamp, sh.sourceIp, sh.destinationIp, dg.sourcePort, dg.destinationPort, dg.data.bytes)

  def v6DataDecode(rh: RecordHeader) = {
    def parse(sh: v6.SimpleHeader) = sh.protocol match {
      case Protocols.Tcp => v6TcpDecode(rh.timestamp, sh)
      case Protocols.Udp => v6UdpDecode(rh.timestamp, sh)
      case _             => provide(data.Unknown)
    }
    for {
      sh <- v6.SimpleHeader.codec
      p <- parse(sh)
    } yield p
  }

  private def frameDecode(rh: RecordHeader) = {
    def parse(efh: EthernetFrameHeader) = efh.ethertype match {
      case Some(t) => t match {
        case EtherType.IPv4 => v4DataDecode(rh)
        case EtherType.IPv6 => v6DataDecode(rh)
        case _              => provide(data.Unknown)
      }
      case None => provide(data.Unknown)
    }
    val c = for {
      efh <- EthernetFrameHeader.codec
      p <- parse(efh)
    } yield p

    new FixedSizeDecoder(rh.includedLength * 8, c)
  }

  def recordDecode(implicit ordering: ByteOrdering) = for {
    rh <- RecordHeader.codec
    p <- frameDecode(rh)
  } yield p

  class Wrapper extends Function0[Decoder[data.Content]] {
    private var ordering: Option[ByteOrdering] = None
    def apply() = {
      ordering match {
        case None => for {
          gh <- GlobalHeader.codec
        } yield {
          ordering = Some(gh.ordering)
          data.Header
        }
        case Some(order) => recordDecode(order)
      }
    }
  }

}