package com.jaymell.ipmapper

import org.junit.jupiter.api.BeforeAll
import java.time.LocalDateTime
import com.mongodb.MongoClient
import org.assertj.core.data.Offset
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import java.time.ZoneOffset


data class TestItem(val Date: LocalDateTime, val testKey: String)

@Configuration
class SpringMongoConfig {

    @Bean
    @Throws(Exception::class)
    fun mongoTemplate(): MongoTemplate {

        return MongoTemplate(MongoClient("127.0.0.1"), "test")

    }

}

class TestMongoDateQuery {

//    @Autowired
//    private lateinit var template: MongoTemplate

    val colName = "test"

    @BeforeAll
    fun insertTestDbItems() {
        val template = MongoTemplate(MongoClient("127.0.0.1"), "james")
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
    }
    @Test
    fun alwaysPassTheTest() {
        println("passed")
    }
}