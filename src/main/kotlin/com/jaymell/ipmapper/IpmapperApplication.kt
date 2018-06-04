package com.jaymell.ipmapper

import com.mongodb.*
import com.mongodb.client.MongoCursor
import org.bson.Document
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.data.mongodb.core.MongoTemplate
import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Bean
import org.springframework.data.mongodb.MongoDbFactory
import org.springframework.data.mongodb.core.MongoClientFactoryBean
import org.springframework.data.mongodb.core.findAll

/*
@Configuration
class AppConfig {
    @Bean
    fun mongo(): MongoClientFactoryBean {
        val mongo = MongoClientFactoryBean()

    }
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
*/

@SpringBootApplication
class IpmapperApplication

fun main(args: Array<String>) {
    runApplication<IpmapperApplication>(*args)
}

@Controller
class JsonController {

    @Autowired
    private lateinit var mongo: MongoClient

    @RequestMapping("/json")
    fun handleRequest(): StreamingResponseBody =
            StreamingResponseBody {
                val out = it
                val template = MongoTemplate(mongo, "logger")
                val results: List<Document> = template.findAll("logs")
                results.forEach{
                            out.write(it.toString().toByteArray(charset("UTF-8")))
                        }
            }
}

