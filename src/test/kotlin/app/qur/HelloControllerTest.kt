package app.qur

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.springframework.test.web.reactive.server.WebTestClient

class HelloControllerTest {

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToController(HelloController()).build()
    }

    @Test
    fun `should return hello world`() {
        webTestClient.get()
            .uri("/hello")
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .isEqualTo("Hello, World!")
    }
}
