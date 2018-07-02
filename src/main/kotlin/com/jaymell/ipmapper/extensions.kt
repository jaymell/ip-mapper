package com.jaymell.ipmapper

import com.maxmind.geoip2.model.InsightsResponse
import java.net.InetAddress

fun InsightsResponse.toIpLocation(ipAddress: InetAddress): IpLocation {
    /*
IpLocation(
        val ip: String,
val latitude: Double,
val longitude: Double,
val countryCode: String,
val city: String,
val country: String
*/
    this.location.
    com.maxmind.geoip2.record.Location
    return IpLocation(ipAddress)
}