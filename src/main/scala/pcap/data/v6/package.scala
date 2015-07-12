package pcap.data

/**
 * @author rsearle
 */
package object v6 {

  import org.joda.time.DateTime
  import scodec.bits.ByteVector

  import scodec.protocols._

  trait Packet extends Data {
    def sourceIp: ip.v6.Address
    def destinationIp: ip.v6.Address
    def key: StreamKey = if (PortOrdering.lt(sourcePort, destinationPort))
      Key(sourcePort, destinationPort, sourceIp, destinationIp)
    else
      Key(destinationPort, sourcePort, destinationIp, sourceIp)
  }

  case class UDP(time: DateTime,
                 sourceIp: ip.v6.Address,
                 destinationIp: ip.v6.Address,
                 sourcePort: ip.Port,
                 destinationPort: ip.Port,
                 bytes: ByteVector) extends Packet

  case class TCP(time: DateTime,
                 sourceIp: ip.v6.Address,
                 destinationIp: ip.v6.Address,
                 sourcePort: ip.Port,
                 destinationPort: ip.Port,
                 sequence: Long,
                 bytes: ByteVector) extends Packet with Sequenced

  private case class Key(portA: ip.Port,
                         portB: ip.Port,
                         ipA: ip.v6.Address,
                         ipB: ip.v6.Address) extends StreamKey
}