package com.jaymell.ipmapper

import java.net.InetAddress

class InvalidIpLocationException(message: String) : Exception(message)

data class IpLocation(
    val ip: InetAddress,
    val latitude: Double?,
    val longitude: Double?,
    val country_iso: String?,
    val city: String?,
    val country: String?)

