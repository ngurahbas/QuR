package app.qur

import app.qur.web.AuthController
import app.qur.web.DashboardController
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.security.test.context.support.WithMockUser
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(SecurityConfig::class)
class AuthControllerTest {

    @Autowired
    private lateinit var context: ApplicationContext

    private lateinit var webTestClient: WebTestClient

    @org.junit.jupiter.api.BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(context).build()
    }

    @Test
    fun `login page should be accessible`() {
        webTestClient.get()
            .uri("/login")
            .exchange()
            .expectStatus().isOk
    }

    @Test
    fun `dashboard should redirect to login for unauthenticated user`() {
        webTestClient.get()
            .uri("/dashboard")
            .exchange()
            .expectStatus().is3xxRedirection
    }

    @Test
    @WithMockUser
    fun `dashboard should be accessible for authenticated user`() {
        webTestClient.get()
            .uri("/dashboard")
            .exchange()
            .expectStatus().isOk
    }
}