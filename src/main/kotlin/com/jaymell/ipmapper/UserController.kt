package com.jaymell.ipmapper

import mu.KLogging
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping

@Controller
@RequestMapping("/users")
@ConditionalOnProperty("enableUserController")
class UserController constructor(
        @Autowired val userRepository: UserRepository,
        val bCryptPasswordEncoder: BCryptPasswordEncoder) {

    companion object : KLogging()

    @PostMapping("/create")
    fun create(@RequestBody user: User): ResponseEntity<HttpStatus> {
        val u = userRepository.findByName(user.name)
        u?.let {
            logger.info("Duplicate user creation attempted")
            return ResponseEntity(HttpStatus.CONFLICT)
        }
        user.password = bCryptPasswordEncoder.encode(user.password)
        userRepository.save(user)
        return ResponseEntity(HttpStatus.OK)
    }
}
