package com.jaymell.ipmapper

import mu.KotlinLogging
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

fun queryMongoByDate(template: MongoTemplate, colName: String, lteParam: Long?, gteParam: Long?): Iterator<Document> {
    val log = KotlinLogging.logger {}

    val nowUtc = LocalDateTime.now(ZoneOffset.UTC)
    val lte = if (lteParam != null) LocalDateTime.ofInstant(Instant.ofEpochMilli(lteParam), ZoneOffset.UTC) else nowUtc
    val gte = if (gteParam != null) LocalDateTime.ofInstant(Instant.ofEpochMilli(gteParam), ZoneOffset.UTC) else lte.minusDays(1)

    log.debug("gte is $gte")
    log.debug("lte is $lte")

    val criteria = Criteria().andOperator(
            Criteria.where("date").lte(lte),
            Criteria.where("date").gte(gte))

    val query = Query().addCriteria(criteria)

    return template.find<Document>(query, colName).iterator()
}