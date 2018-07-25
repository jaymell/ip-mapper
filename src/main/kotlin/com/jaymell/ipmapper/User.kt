package com.jaymell.ipmapper

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.repository.MongoRepository

class User(val name: String, var password: String) {
    @Id
    lateinit var id: String
}

interface UserRepository : MongoRepository<User, String> {
    fun findByName(name: String): User?
}
