package asura.common.util

import java.net.{InetAddress, NetworkInterface}

import scala.jdk.CollectionConverters._

object NetworkUtils {

  def getLocalIpAddress(): String = {
    val interfaces = NetworkInterface.getNetworkInterfaces.asScala.toSeq
    val ipAddresses = interfaces.flatMap(_.getInetAddresses.asScala.toSeq)
    val address = ipAddresses.find(address => {
      val host = address.getHostAddress
      host.contains(".") && !address.isLoopbackAddress
    }).getOrElse(InetAddress.getLocalHost)
    address.getHostAddress
  }
}
