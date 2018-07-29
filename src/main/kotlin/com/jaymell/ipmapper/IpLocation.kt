package com.jaymell.ipmapper

class InvalidIpLocationException(message: String) : Exception(message)

data class IpLocation(
        val ip: String,
        val latitude: Double?,
        val longitude: Double?,
        val country_iso: String?,
        val city: String?,
        val country: String?)

