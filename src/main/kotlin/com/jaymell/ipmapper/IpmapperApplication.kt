package com.jaymell.ipmapper

import com.mongodb.DB
import com.mongodb.DBCollection
import com.mongodb.DBObject
import com.mongodb.client.MongoCursor
import org.bson.Document
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.data.mongodb.core.MongoTemplate
import com.mongodb.MongoClient
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.springframework.context.annotation.Bean


@Configuration
class AppConfig {

    val mongo = MongoClient("localhost", 27017)
    val dbName = "logger"
    val colName = "logs"

    @Bean
    @Throws(Exception::class)
    fun mongoTemplate(): MongoTemplate {
        return MongoTemplate(mongo,dbName)
    }

    @Bean
    fun db(): MongoDatabase = mongo.getDatabase(dbName)

    @Bean
    fun collection(): MongoCollection<Document> = db().getCollection(colName)
}

@SpringBootApplication
class IpmapperApplication

fun main(args: Array<String>) {
    runApplication<IpmapperApplication>(*args)
}

@Controller
class JsonController {
    @RequestMapping("/json")
    fun handleRequest(): StreamingResponseBody =
            StreamingResponseBody {
                val out = it
//                val template = AppConfig().mongoTemplate()
//                val results = template.findAll(DBObject::class.java, "logs")
                val col = AppConfig().collection()
                val cursor: MongoCursor<Document> = col.find().iterator()
                cursor.forEach {
                    out.write(it.toString().toByteArray(charset("UTF-8")))
                }
//                results.forEach {
//                    out.write(it.toString().toByteArray(charset("UTF-8")))
//                }
            }
}

