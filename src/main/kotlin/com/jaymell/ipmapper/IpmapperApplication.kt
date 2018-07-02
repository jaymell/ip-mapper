package com.jaymell.ipmapper

import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class AppConfig {
    @Value("\${maxmind.key}")
    lateinit var maxmindKey: String

    @Value("\${maxmind.accountId}")
    lateinit var maxmindAccountId: String

    @Bean
    fun geolocator(): MaxmindGeolocator = MaxmindGeolocator(maxmindAccountId, maxmindKey)
}

@SpringBootApplication
class IpmapperApplication

fun main(args: Array<String>) {
    runApplication<IpmapperApplication>(*args)
}
