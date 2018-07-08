package com.jaymell.ipmapper

import com.maxmind.geoip2.model.InsightsResponse

fun InsightsResponse.toIpLocation(ip: String): IpLocation =
        IpLocation(ip,
                this.location.latitude,
                this.location.longitude,
                this.country.isoCode,
                this.city.names["en"],
                this.country.names["en"])
