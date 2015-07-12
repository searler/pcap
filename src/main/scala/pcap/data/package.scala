package pcap

import org.joda.time.DateTime
import scodec.bits.ByteVector

import scodec.protocols._

/**
 * @author rsearle
 */
package object data {

  sealed trait Content

  case object Header extends Content

  case object Unknown extends Content

  trait Data extends Content {
    def time: DateTime
    def bytes: ByteVector
    def sourcePort: ip.Port
    def destinationPort: ip.Port
  }

  trait Sequenced {
    def sequence: Long
  }

  trait StreamKey

}