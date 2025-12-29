package app.qur.web

import app.qur.db.TestContainersConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import org.testcontainers.junit.jupiter.Testcontainers

@SpringBootTest
@Import(TestContainersConfig::class)
@Testcontainers
class AuthControllerTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build()
    }

    @Test
    fun `logins endpoint should return correct response with all oauth providers`() {
        val body = webTestClient.get()
            .uri("/logins")
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)
            .responseBody
            .collectList()
            .block()
            ?.joinToString("") ?: ""

        assert(body.contains("Keycloak")) { "Response body should contain 'Keycloak'" }
        assert(body.contains("/oauth2/authorization/keycloak")) { "Response body should contain OAuth2 authorization URL" }
        assert(body.contains("Continue with")) { "Response body should contain 'Continue with'" }
    }
}