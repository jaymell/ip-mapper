package com.jaymell.ipmapper

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.SignatureAlgorithm
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import java.io.IOException
import java.util.*
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import mu.KLogging

class JwtAuthenticationFilter(val authManager: AuthenticationManager)
    : UsernamePasswordAuthenticationFilter() {

    companion object : KLogging()

    override fun attemptAuthentication(request: HttpServletRequest?, response: HttpServletResponse?): Authentication {
        try {
            val mapper = jacksonObjectMapper()
            val creds: User = mapper
                    .readValue(request?.inputStream?.bufferedReader().use { it?.readText()}!!)

            return authManager.authenticate(
                    UsernamePasswordAuthenticationToken(creds.name, creds.password, ArrayList())
            )
        } catch (e: IOException) {
            logger.debug("$e")
            throw RuntimeException()
        }
    }

    override fun successfulAuthentication(request: HttpServletRequest?,
                                          response: HttpServletResponse?,
                                          chain: FilterChain?,
                                          authResult: Authentication?) {
        val u = authResult?.principal as org.springframework.security.core.userdetails.User
        val token = Jwts.builder()
                .setSubject(u.username)
                .setExpiration(Date(System.currentTimeMillis() + SecurityConstants.EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SecurityConstants.SECRET.toByteArray())
                .compact()
        response?.addHeader(SecurityConstants.HEADER_STRING, SecurityConstants.TOKEN_PREFIX + token)
    }
}

