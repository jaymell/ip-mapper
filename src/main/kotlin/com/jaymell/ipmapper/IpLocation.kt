package com.jaymell.ipmapper

import java.net.InetAddress

class InvalidIpLocationException(message: String) : Exception(message)

data class IpLocation(
    val ip: InetAddress,
    val latitude: Double?,
    val longitude: Double?,
    val countryCode: String?,
    val city: String?,
    val country: String?) {

    override fun toString(): String = "ip: $ip, latitude: $latitude, longitude: $longitude, countryCode: $countryCode, city: $city, country: $country"
}

