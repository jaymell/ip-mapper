package com.jaymell.ipmapper

import com.maxmind.geoip2.WebServiceClient
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import java.net.InetAddress


class MaxmindGeolocator(val maxmindAccountId: Int, val maxmindKey: String) : Geolocator {

    companion object : KLogging()

    @Autowired
    override lateinit var cache: IpLocationCache

    val client = WebServiceClient.Builder(maxmindAccountId, maxmindKey).build()

    override fun geolocate(ip: String): IpLocation {
        try {
            val ipAddress = InetAddress.getByName(ip)
            if (!org.apache.commons.validator.routines.InetAddressValidator().isValid(ip)) {
                throw InvalidIpLocationException("invalid ip address")
            }
            try {
                val cachedIpLocation = cache.get(ip)
                if (cachedIpLocation != null) return cachedIpLocation
                // else query maxmind:
                val resp = client.insights(ipAddress)
                val respIpLocation = resp.toIpLocation(ip)
                try {
                    cache.put(respIpLocation)
                } catch (e: Exception) {
                    logger.error("Unable to insert record into cache")
                    throw e
                }
                return respIpLocation
            } catch (e: Exception) {
                logger.error("Failed to query cache")
                throw e
            }
        } catch (e: java.net.UnknownHostException) {
            throw InvalidIpLocationException("invalid ip address")
        }

    }
}