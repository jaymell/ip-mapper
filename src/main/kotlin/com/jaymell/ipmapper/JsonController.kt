package com.jaymell.ipmapper

import com.mongodb.MongoClient
import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@Controller
class JsonController {

    companion object : KLogging()

    @Autowired
    private lateinit var mongo: MongoClient

    val dbName = "logger"
    val colName = "logs"

    @RequestMapping("/json", method = [RequestMethod.GET])
    fun handleRequest(@RequestParam(value = "gte", required = false) gteParam: Long?,
                      @RequestParam(value = "lte", required = false) lteParam: Long?): StreamingResponseBody {

        val template = MongoTemplate(mongo, dbName)

        return StreamingResponseBody { out ->
            queryMongoByDate(template, colName, lteParam, gteParam)
                    .forEach {
                        out.write("${com.mongodb.util.JSON.serialize(it)}\n".toByteArray())
                    }
        }
    }
}