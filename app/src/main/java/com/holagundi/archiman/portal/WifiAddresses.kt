package com.holagundi.archiman.portal

import java.net.Inet4Address
import java.net.NetworkInterface

object WifiAddresses {
    fun current(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()?.toList().orEmpty()
        val candidates = interfaces.filter { it.isUp && !it.isLoopback }.flatMap { nif ->
            nif.inetAddresses.toList().mapNotNull { address ->
                if (address is Inet4Address && !address.isLoopbackAddress && !address.isLinkLocalAddress) {
                    nif.name to address.hostAddress.orEmpty()
                } else {
                    null
                }
            }
        }.filter { it.second.isNotBlank() }
        return PortalHttp.chooseAddress(candidates)
    }
}
