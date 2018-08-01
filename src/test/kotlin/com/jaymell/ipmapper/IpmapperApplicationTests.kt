package com.jaymell.ipmapper

import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@ExtendWith(SpringExtension::class)
@SpringBootTest(properties = ["enableUserController=false"])
@AutoConfigureMockMvc
class TestUserControllerDoesntLoad {

    @Autowired
    lateinit var mvc: MockMvc

    @Test
    fun should_return_404() {
        this.mvc.perform(post("/users/create")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"name\": \"test\", \"password\": \"password\" }"))
                .andExpect(status().isNotFound)
    }

}

@ExtendWith(SpringExtension::class)
@SpringBootTest(properties = ["enableUserController=true"])
@AutoConfigureMockMvc
class TestUserControllerLoads {

    @Autowired
    lateinit var mvc: MockMvc

    @Autowired
    private lateinit var template: MongoTemplate

    @Test
    fun should_return_409_maybe() {
        this.mvc.perform(post("/users/create")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"name\": \"test\", \"password\": \"password\" }"))
                .andExpect(status().is2xxSuccessful)
    }

    @AfterAll
    fun cleanUp() {
        template.dropCollection("user")
    }
}
