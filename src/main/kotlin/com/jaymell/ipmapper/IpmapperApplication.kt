package com.jaymell.ipmapper

import com.mongodb.MongoClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories

@Configuration
class AppConfig {
    @Value("\${maxmind.key}")
    lateinit var maxmindKey: String

    @Value("\${maxmind.accountId}")
    lateinit var maxmindAccountId: String

    @Bean
    fun geolocator(): MaxmindGeolocator = MaxmindGeolocator(maxmindAccountId.toInt(), maxmindKey)

    @Bean
    fun cache(): MongoIpLocationCache = MongoIpLocationCache()
}

@Configuration
@EnableMongoRepositories(basePackages = ["com.jaymell.ipmapper"])
class MongoConfig {

    @Autowired
    lateinit var mongo: MongoClient

    @Bean
    @Qualifier("cache")
    fun mongoTemplate(): MongoTemplate {
        return MongoTemplate(mongo, "cache")
    }
}

@SpringBootApplication
class IpmapperApplication

fun main(args: Array<String>) {
    runApplication<IpmapperApplication>(*args)
}
