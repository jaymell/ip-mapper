package com.jaymell.ipmapper.securityconstants

import org.springframework.beans.factory.annotation.Value

@Value("\${jwt.key}")
lateinit var SECRET: String

val EXPIRATION_TIME: Long = 864000000 // 10 days
val TOKEN_PREFIX = "Bearer "
val HEADER_STRING = "Authorization"
val SIGN_UP_URL = "/users/create"
