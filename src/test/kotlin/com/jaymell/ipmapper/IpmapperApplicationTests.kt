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
import org.assertj.core.api.Assertions.assertThat
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.test.autoconfigure.data.mongo.AutoConfigureDataMongo
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.context.annotation.Import
import org.springframework.context.annotation.Profile
import org.springframework.data.mongodb.repository.MongoRepository
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource


@Configuration
@Profile("test")
class TestAppConfig {
    @Value("\${maxmind.key}")
    lateinit var maxmindKey: String

    @Value("\${maxmind.accountId}")
    lateinit var maxmindAccountId: String

    @Bean
    fun geolocator(): MaxmindGeolocator = MaxmindGeolocator(maxmindAccountId.toInt(), maxmindKey)

    @Bean
    fun cache(): MongoIpLocationCache = MongoIpLocationCache()

    @Bean
    fun bCryptPasswordEncoder() = BCryptPasswordEncoder()

    @Bean
    fun corsConfigurationService(): CorsConfigurationSource {
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", CorsConfiguration().applyPermitDefaultValues())
        return source
    }
}

@Configuration
@Profile("test")
class TestMongoConfig {

    @Autowired
    lateinit var mongo: MongoClient

    @Bean(name = ["cacheTemplate"])
    fun mongoTemplate(): MongoTemplate {
        return MongoTemplate(mongo, "test")
    }
}

@Configuration
@Profile("test")
@EnableMongoRepositories(basePackages = ["com.jaymell.ipmapper"])
class TestMongoRepoConfig {

    @Autowired
    lateinit var mongo: MongoClient

    @Bean
    fun mongoTemplate(): MongoTemplate {
        return MongoTemplate(mongo, "test")
    }
}

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

    /*
    @MockBean
    lateinit var userRepository: UserRepository

    @MockBean
    lateinit var ipLocationCacheRepository: IpLocationCacheRepository


    @MockBean(name="cacheTemplate")
    lateinit var cacheTemplate: MongoTemplate

    @MockBean(name="mongoTemplate")
    lateinit var mongoTemplate: MongoTemplate

    @MockBean
     */
    val template = MongoTemplate(MongoClient(), "test")

    @Test
    fun should_return_200() {
//        given(userRepository.findByName("test")).willReturn(User("james", "password"))
//        given(userRepository.save(User("test", "password"))).willReturn(null)
        mvc.perform(post("/users/create")
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

@SpringBootTest
@ExtendWith(SpringExtension::class)
class TestAuth {

    @Test
    fun security_constants_should_not_have_default_value_set() {
        assertThat(SecurityConstants.SECRET).isNotEqualTo(SecurityConstants.defaultSecret)
    }
}
