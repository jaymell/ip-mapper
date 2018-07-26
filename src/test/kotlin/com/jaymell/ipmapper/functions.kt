package com.jaymell.ipmapper

import com.mongodb.MongoClient
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDateTime
import java.time.ZoneOffset


data class TestItem(val date: LocalDateTime, val testKey: String)

@Configuration
class MongoTestConfig {

    @Autowired
    lateinit var mongo: MongoClient

    @Bean(name=["testTemplate"])
    fun mongoTemplate(): MongoTemplate {
        return MongoTemplate(mongo, "test")
    }
}

@ExtendWith(SpringExtension::class)
@SpringBootTest
class TestMongoDateQuery {

    @Autowired
    @Qualifier("testTemplate")
    private lateinit var template: MongoTemplate

    val colName = "test"

    val now = LocalDateTime.now(ZoneOffset.UTC)
    val oneHourAgo = now.minusHours(1)
    val twoDaysAgo = now.minusDays(2)
    val oneYearAgo = now.minusYears(1)

    @BeforeAll
    fun insertTestDbItems() {
        template.insert(TestItem(now, "testValue1"), colName)
        template.insert(TestItem(oneHourAgo, "testValue2"), colName)
        template.insert(TestItem(twoDaysAgo, "testValue3"), colName)
        template.insert(TestItem(oneYearAgo, "testValue4"), colName)
    }

    @AfterAll
    fun cleanUp() {
        template.dropCollection(colName)
    }

    @Test
    fun should_return_results_from_last_24_hours_if_no_params_passed() {
        val results = queryMongoByDate(template, colName, null, null)
                .asSequence()
                .toList()
        assertThat(results.size).isEqualTo(2)
    }

    @Test
    fun should_return_expected_results_if_only_lte_param_is_passed() {
        val results = queryMongoByDate(template,
                colName,
                now.minusDays(2).toInstant(ZoneOffset.UTC).toEpochMilli(),
                null)
                .asSequence()
                .toList()
        println("two days ago ${twoDaysAgo}")
        assertThat(results.size).isEqualTo(1)
    }

    @Test
    fun should_return_expected_results_if_only_gte_param_is_passed() {
        val results = queryMongoByDate(template,
                colName,
                null,
                now.minusHours(2).toInstant(ZoneOffset.UTC).toEpochMilli())
                .asSequence()
                .toList()
        assertThat(results.size).isEqualTo(2)
    }

    @Test
    fun should_return_expected_results_if_both_lte_and_gte_are_passed() {
        val results = queryMongoByDate(template,
                colName,
                now.minusDays(1).toInstant(ZoneOffset.UTC).toEpochMilli(),
                now.minusDays(3).toInstant(ZoneOffset.UTC).toEpochMilli())
                .asSequence()
                .toList()
        assertThat(results.size).isEqualTo(1)
    }
}