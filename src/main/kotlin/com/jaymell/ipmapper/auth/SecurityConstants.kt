package com.jaymell.ipmapper

import org.springframework.beans.factory.annotation.Value

class SecurityConstants {
    companion object {
        @Value("\${jwt.key}")
        lateinit var SECRET: String
        val EXPIRATION_TIME: Long = 864000000 // 10 days
        val TOKEN_PREFIX = "Bearer "
        val HEADER_STRING = "Authorization"
        val SIGN_UP_URL = "/users/create"
    }
}