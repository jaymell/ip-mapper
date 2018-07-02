package com.jaymell.ipmapper

interface Geolocator {
    fun geolocate(ip: String): IpLocation
}