package com.jaymell.ipmapper

import org.junit.jupiter.api.BeforeAll
import java.time.LocalDateTime
import com.mongodb.MongoClient
import org.assertj.core.api.Assertions.assertThat
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


data class TestItem(val date: LocalDateTime, val testKey: String)

@ExtendWith(SpringExtension::class)
@SpringBootTest
class TestMongoDateQuery {

    @Autowired
    private lateinit var template: MongoTemplate

    val colName = "test"

    @BeforeAll
    fun insertTestDbItems() {
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC), "testValue1"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC).minusHours(1), "testValue2"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC).minusDays(2), "testValue3"), colName)
        template.insert(TestItem(LocalDateTime.now(ZoneOffset.UTC).minusYears(1), "testValue4"), colName)
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

    }

    @Test
    fun should_return_expected_results_if_only_gte_param_is_passed() {

    }

    @Test
    fun should_return_expected_results_if_both_lte_and_gte_are_passed() {

    }
}