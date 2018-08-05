package com.jaymell.ipmapper

import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SecurityConstants {
    companion object {
        val defaultSecret = "defaultKey"

        @JvmField
        var SECRET: String = defaultSecret

        val EXPIRATION_TIME: Long = 864000000 // 10 days
        val TOKEN_PREFIX = "Bearer "
        val HEADER_STRING = "Authorization"
        val SIGN_UP_URL = "/users/create"
    }

    @Value("\${jwt.key}")
    fun setSECRET(secret: String) {
        SECRET = secret
    }
}
