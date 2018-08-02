package com.jaymell.ipmapper

import com.mongodb.MongoClient
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.http.MediaType
import org.springframework.test.context.junit.jupiter.SpringExtension
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.io.IOException
import com.mongodb.Mongo
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories


@SpringBootTest(properties = ["enableUserController=false"])
@ExtendWith(SpringExtension::class)
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

@SpringBootTest(properties = ["enableUserController=true"])
@ExtendWith(SpringExtension::class)
@AutoConfigureMockMvc
class TestUserControllerLoads {

    @Autowired
    lateinit var mvc: MockMvc

    @MockBean
    val template: MongoTemplate = MongoTemplate(MongoClient(), "test")

    @Test
    fun should_return_200() {
        this.mvc.perform(post("/users/create")
                .accept(MediaType.APPLICATION_JSON)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{ \"name\": \"test\", \"password\": \"password\" }"))
                .andExpect(status().is2xxSuccessful)
    }

    @AfterAll
    fun cleanup() {
        template.dropCollection("user")
    }
}
