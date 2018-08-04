package com.jaymell.ipmapper

import com.jaymell.ipmapper.securityconstants.*
import io.jsonwebtoken.Jwts
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter
import javax.servlet.FilterChain
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class JwtAuthorizationFilter(authManager: AuthenticationManager) : BasicAuthenticationFilter(authManager) {

    override fun doFilterInternal(request: HttpServletRequest, response: HttpServletResponse, chain: FilterChain) {
        val header = request.getHeader(HEADER_STRING)
        if (header != null && ! header.startsWith(TOKEN_PREFIX)) {
            val authentication = getAuthentication(request)
            authentication?.let {
                SecurityContextHolder.getContext().authentication = it
            }
        }
        chain.doFilter(request, response)
        return
    }

    fun getAuthentication(request: HttpServletRequest): UsernamePasswordAuthenticationToken? {
        val token = request.getHeader(HEADER_STRING)
        token?.let {
            val user = Jwts.parser()
                    .setSigningKey(SECRET.toByteArray())
                    .parseClaimsJws(token.replace(TOKEN_PREFIX, ""))
                    .body
                    .subject
            user?.let {
                return UsernamePasswordAuthenticationToken(user, null, ArrayList())
            }
        }
        return null
    }
}