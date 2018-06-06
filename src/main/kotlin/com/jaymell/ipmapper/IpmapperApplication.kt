package com.jaymell.ipmapper

import com.mongodb.*
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.beans.factory.annotation.Autowired

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
            StreamingResponseBody { out ->
                val template = MongoTemplate(mongo, "logger")
                val col = template.getCollection("logs")
                col.find().iterator().forEach {
                    out.write("${it.toJson()}\n".toByteArray())
                }
            }
}

