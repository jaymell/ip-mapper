package com.jaymell.ipmapper

import java.time.Instant

data class CacheItem<T>(val item: T) {
    val time = Instant.now()
}

interface IpLocationCache {
    fun put(ipLocation: IpLocation)
    fun get(ip: String): IpLocation?
}