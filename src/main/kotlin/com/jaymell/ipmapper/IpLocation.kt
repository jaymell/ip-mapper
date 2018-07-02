package com.jaymell.ipmapper

data class IpLocation(
    val ip: String,
    val latitude: Double,
    val longitude: Double,
    val countryCode: String,
    val city: String,
    val country: String
)
