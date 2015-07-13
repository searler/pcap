package pcap.data

import org.joda.time.DateTime
import scodec.bits.ByteVector
package object v4 {

  import scodec.protocols._

  trait Packet extends Data {
    def sourceIp: ip.v4.Address
    def destinationIp: ip.v4.Address

    def stream: StreamKey = if (PortOrdering.lt(sourcePort, destinationPort))
      Key(sourcePort, destinationPort, sourceIp, destinationIp)
    else
      Key(destinationPort, sourcePort, destinationIp, sourceIp)
  }

  case class UDP(time: DateTime,
                 sourceIp: ip.v4.Address,
                 destinationIp: ip.v4.Address,
                 sourcePort: ip.Port,
                 destinationPort: ip.Port,
                 bytes: ByteVector) extends Packet

  case class TCP(time: DateTime,
                 sourceIp: ip.v4.Address,
                 destinationIp: ip.v4.Address,
                 sourcePort: ip.Port,
                 destinationPort: ip.Port,
                 sequence: Long,
                 bytes: ByteVector) extends Packet with Sequenced

  private case class Key(portA: ip.Port,
                         portB: ip.Port,
                         ipA: ip.v4.Address,
                         ipB: ip.v4.Address) extends StreamKey

}