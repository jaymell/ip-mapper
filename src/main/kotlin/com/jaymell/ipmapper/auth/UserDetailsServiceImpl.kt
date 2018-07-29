package com.jaymell.ipmapper

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsServiceImpl(val userRepository: UserRepository) : UserDetailsService {
    override fun loadUserByUsername(username: String?): UserDetails {
        username?.let {
            val user = userRepository.findByName(it)
            user?.let {
                return org.springframework.security.core.userdetails.User(user.name, user.password, ArrayList())
            }
        }
        throw UsernameNotFoundException(username)
    }
}

