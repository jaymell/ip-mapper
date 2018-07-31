package com.jaymell.ipmapper

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.repository.NoRepositoryBean

class User(@Id val name: String, var password: String)

interface UserRepository : MongoRepository<User, String> {
    fun findByName(name: String): User?
}

