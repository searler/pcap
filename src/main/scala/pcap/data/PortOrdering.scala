package pcap.data

import scodec.protocols._

/**
 * @author rsearle
 */
object PortOrdering extends Ordering[ip.Port] {
    def compare(a: ip.Port, b: ip.Port) = a.value compare b.value
  }