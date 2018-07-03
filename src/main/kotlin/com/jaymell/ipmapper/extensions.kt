package com.jaymell.ipmapper

import com.maxmind.geoip2.model.InsightsResponse
import java.net.InetAddress

fun InsightsResponse.toIpLocation(ipAddress: InetAddress): IpLocation =
        IpLocation(ipAddress,
                this.location.latitude,
                this.location.longitude,
                this.country.isoCode,
                this.city.names["en"],
                this.country.names["en"])
