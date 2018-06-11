package com.jaymell.ipmapper

import com.mongodb.MongoClient
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.find
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

@SpringBootApplication
class IpmapperApplication

fun main(args: Array<String>) {
    runApplication<IpmapperApplication>(*args)
}

fun queryMongoByDate(template: MongoTemplate, colName: String, lteParam: Long?, gteParam: Long?): Iterator<Document> {
    val nowUtc = LocalDateTime.now(ZoneOffset.UTC)
    val lte = if (lteParam != null) LocalDateTime.ofInstant(Instant.ofEpochMilli(lteParam), ZoneOffset.UTC) else nowUtc
    val gte = if (gteParam != null) LocalDateTime.ofInstant(Instant.ofEpochMilli(gteParam), ZoneOffset.UTC) else nowUtc.minusDays(1)

    val criteria = Criteria().andOperator(
            Criteria.where("date").lte(lte),
            Criteria.where("date").gte(gte))

    val query = Query().addCriteria(criteria)

    template.find<Document>(query, colName).iterator()
}

@Controller
class JsonController {

    @Autowired
    private lateinit var mongo: MongoClient

    val colName = "logs"

    @RequestMapping("/json", method = [RequestMethod.GET])
    fun handleRequest(@RequestParam(value = "gte", required = false) gteParam: Long?,
                      @RequestParam(value = "lte", required = false) lteParam: Long?): StreamingResponseBody {

        val template = MongoTemplate(mongo, "logger")

        return StreamingResponseBody { out ->
            queryMongoByDate(template, colName, lteParam, gteParam)
                    .forEach {
                        out.write("${it.toJson()}\n".toByteArray())
                    }
        }
    }
}
