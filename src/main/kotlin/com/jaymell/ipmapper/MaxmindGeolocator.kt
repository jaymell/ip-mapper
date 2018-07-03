package com.jaymell.ipmapper

import com.maxmind.geoip2.WebServiceClient
import java.net.InetAddress


class MaxmindGeolocator(val maxmindAccountId: Int, val maxmindKey: String) : Geolocator {

    val client = WebServiceClient.Builder(maxmindAccountId, maxmindKey).build()

    override fun geolocate(ip: String): IpLocation {
        try {
            val ipAddress = InetAddress.getByName(ip)
            if (!org.apache.commons.validator.routines.InetAddressValidator().isValid(ip)) {
                throw InvalidIpLocationException("invalid ip address")
            }
            val resp = client.insights(ipAddress)
            return resp.toIpLocation(ipAddress)
        } catch (e: java.net.UnknownHostException) {
            throw InvalidIpLocationException("invalid ip address")
        }

    }
}