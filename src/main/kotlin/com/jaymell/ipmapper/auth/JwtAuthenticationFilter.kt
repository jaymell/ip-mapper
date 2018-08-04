package com.jaymell.ipmapper

import com.fasterxml.jackson.databind.ObjectMapper
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
import com.jaymell.ipmapper.securityconstants.*

class JwtAuthenticationFilter(private val authManager: AuthenticationManager)
    : UsernamePasswordAuthenticationFilter() {

    override fun attemptAuthentication(request: HttpServletRequest?, response: HttpServletResponse?): Authentication {
		println("this request: $request")
        try {
            val creds: User = ObjectMapper()
                    .readValue(request?.inputStream, User::class.java)
            return authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken(creds.name, creds.password, ArrayList())
            )
        } catch (e: IOException) {
            throw RuntimeException()
        }
    }

    override fun successfulAuthentication(request: HttpServletRequest?,
                                          response: HttpServletResponse?,
                                          chain: FilterChain?,
                                          authResult: Authentication?) {
        val u: User = authResult?.principal as User
        val token = Jwts.builder()
                .setSubject(u.name)
                .setExpiration(Date(System.currentTimeMillis() + EXPIRATION_TIME))
                .signWith(SignatureAlgorithm.HS512, SECRET.toByteArray())
                .compact()
        response?.addHeader(HEADER_STRING, TOKEN_PREFIX + token)
    }
}
