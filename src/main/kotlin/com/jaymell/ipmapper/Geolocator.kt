package com.jaymell.ipmapper

interface Geolocator {
    val cache: IpLocationCache
    fun putCache(ipLocation: IpLocation) = cache.put(ipLocation)
    fun getCache(ip: String): IpLocation? = cache.get(ip)
    fun geolocate(ip: String): IpLocation
}