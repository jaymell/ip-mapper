package com.jaymell.ipmapper

import org.junit.jupiter.api.BeforeAll
import java.time.LocalDateTime
import com.mongodb.MongoClient
import org.assertj.core.data.Offset
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.ZoneOffset


data class TestItem(val Date: LocalDateTime, val testKey: String)

@ExtendWith(SpringExtension::class)
@SpringBootTest
class TestMongoDateQuery {

    @Autowired
    private lateinit var template: MongoTemplate

    val dbName = "testing"
    val colName = "test"

    @BeforeAll
    fun insertTestDbItems() {
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
    }

    @AfterAll
    fun cleanUp() {
        template.dropCollection(colName)
    }

    @Test
    fun alwaysPassTheTest() {
        println("passed")
    }
}