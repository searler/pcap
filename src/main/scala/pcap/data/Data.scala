package pcap.data

import org.joda.time.DateTime
import scodec.bits.ByteVector

import scodec.protocols.ip._

/**
 * @author rsearle
 */
object Data {
  sealed trait Packet

  case object Header extends Packet

  case object Unknown extends Packet

  trait Data extends Packet {
    def time: DateTime
    def bytes: ByteVector
    def sourcePort: Port
    def destinationPort: Port
  }

  trait v4Data extends Data {
    def sourceIp: v4.Address
    def destinationIp: v4.Address
  }

  trait v6Data extends Data {
    def sourceIp: v6.Address
    def destinationIp: v6.Address
  }

  case class UDP(time: DateTime, sourceIp: v4.Address,
                 destinationIp: v4.Address, sourcePort: Port,
                 destinationPort: Port, bytes: ByteVector) extends v4Data

  case class UDPv6(time: DateTime, sourceIp: v6.Address,
                   destinationIp: v6.Address, sourcePort: Port,
                   destinationPort: Port, bytes: ByteVector) extends v6Data

  case class TCP(time: DateTime,
                 sourceIp: v4.Address,
                 destinationIp: v4.Address, sourcePort: Port,
                 destinationPort: Port, sequence: Long, bytes: ByteVector) extends v4Data

  case class TCPv6(time: DateTime,
                   sourceIp: v6.Address,
                   destinationIp: v6.Address, sourcePort: Port,
                   destinationPort: Port, sequence: Long, bytes: ByteVector) extends v6Data

}