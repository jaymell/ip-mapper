package com.jaymell.ipmapper

import com.maxmind.geoip2.WebServiceClient
import java.net.InetAddress

class MaxmindGeolocator(val maxmindAccountId: String, val maxmindKey: String) : Geolocator {

    val client = WebServiceClient.Builder(maxmindAccountId, maxmindKey).build()

    override fun geolocate(ip: String): IpLocation {
        val ipAddress = InetAddress.getByName(ip)
        val resp = client.insights(ipAddress)
        return IpLocation()

        return
    }
}