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
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

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

    @Bean
    fun bCryptPasswordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationService(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", CorsConfiguration().applyPermitDefaultValues())
        return source
    }
}

@Configuration
class MongoConfig {

    @Autowired
    lateinit var mongo: MongoClient

    @Bean(name=["cacheTemplate"])
    fun mongoTemplate(): MongoTemplate {
        return MongoTemplate(mongo, "cache")
    }
}

@Configuration
@EnableMongoRepositories(basePackages = ["com.jaymell.ipmapper"])
class MongoRepoConfig {

    @Autowired
    lateinit var mongo: MongoClient

    @Bean
    fun mongoTemplate(): MongoTemplate {
        return MongoTemplate(mongo, "user")
    }
}

@SpringBootApplication
class IpmapperApplication

fun main(args: Array<String>) {
    runApplication<IpmapperApplication>(*args)
}
