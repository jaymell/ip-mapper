package com.jaymell.ipmapper

import mu.KLogging
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.findOne
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import org.springframework.data.mongodb.core.query.Update
import org.springframework.data.repository.PagingAndSortingRepository
import java.time.LocalDateTime
import java.time.OffsetTime

interface IpLocationCacheRepository : PagingAndSortingRepository<CacheItem<IpLocation>, String> {
    fun findByTime(time: OffsetTime): CacheItem<IpLocation>?
}

class MongoIpLocationCache : IpLocationCache {

    companion object : KLogging()

    @Autowired
    @Qualifier("cacheTemplate")
    lateinit var template: MongoTemplate

    override fun put(ipLocation: IpLocation) {
        val q = Query().addCriteria(Criteria.where("item.ip").`is`(ipLocation.ip))
        val dbDoc = Document()
        val ipCacheItem = CacheItem(ipLocation)
        template.converter.write(ipCacheItem, dbDoc)
        val u = Update.fromDocument(dbDoc, "_id")
        template.upsert(q, u, CacheItem::class.java)
    }

    override fun get(ip: String): IpLocation? {
        val oneMonthAgo = LocalDateTime.now().minusMonths(1)

        val criteria = Criteria().andOperator(
                Criteria.where("item.ip").`is`(ip),
                Criteria.where("time").gte(oneMonthAgo))

        val q = Query().addCriteria(criteria)
        val resp = template.findOne<CacheItem<IpLocation>>(q, "cacheItem")
        if (resp == null) {
            logger.debug("IP $ip NOT FOUND in cache")
            return null
        }
        logger.debug("IP $ip FOUND in cache")
        return resp.item
    }
}
