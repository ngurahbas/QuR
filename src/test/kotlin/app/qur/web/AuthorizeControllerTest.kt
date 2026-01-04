package app.qur.web

import app.qur.security.JwtService
import app.qur.service.ApprovalService
import app.qur.service.DeviceRole
import app.qur.test.IntegrationTestConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.web.reactive.server.WebTestClient

@SpringBootTest
@Import(IntegrationTestConfig::class)
class AuthorizeControllerTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var approvalService: ApprovalService

    @Autowired
    private lateinit var jwtService: JwtService

    @Autowired
    private lateinit var redisTemplate: ReactiveStringRedisTemplate

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build()
        
        // Clean up Redis
        redisTemplate.execute { connection ->
            connection.serverCommands().flushDb()
        }.blockLast()
    }

    private fun createAuthToken(): String {
        // Create a mock OidcUser for testing
        val claims = mapOf(
            "sub" to "test@example.com",
            "email" to "test@example.com",
            "name" to "Test User"
        )
        val oidcUser = org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser(
            listOf(org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_USER")),
            org.springframework.security.oauth2.core.oidc.OidcIdToken.withTokenValue("test-token")
                .claim("sub", "test@example.com")
                .claim("email", "test@example.com")
                .claim("name", "Test User")
                .build()
        )
        return jwtService.generateToken(oidcUser)
    }

    @Test
    fun `POST authorize qr should succeed with valid approval_id and role`() {
        val deviceId = "test-device-123"
        val approvalId = approvalService.createApproval(deviceId).block()!!
        val token = createAuthToken()

        webTestClient.post()
            .uri("/authorize/qr")
            .cookie("jwt", token)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("approval_id=$approvalId&new_role=QUEUE_DISPLAY")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("success")
    }

    @Test
    fun `POST authorize qr should fail with invalid approval_id`() {
        val token = createAuthToken()

        webTestClient.post()
            .uri("/authorize/qr")
            .cookie("jwt", token)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("approval_id=invalid-approval-id&new_role=QUEUE_DISPLAY")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo("error")
            .jsonPath("$.message").isEqualTo("Approval ID not found")
    }

    @Test
    fun `POST authorize qr should fail with SETUP role`() {
        val deviceId = "test-device-456"
        val approvalId = approvalService.createApproval(deviceId).block()!!
        val token = createAuthToken()

        webTestClient.post()
            .uri("/authorize/qr")
            .cookie("jwt", token)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("approval_id=$approvalId&new_role=SETUP")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo("error")
            .jsonPath("$.message").isEqualTo("Role must be QUEUE_TAKING_QR_TARGET or QUEUE_DISPLAY")
    }

    @Test
    fun `POST authorize qr should fail with invalid role`() {
        val deviceId = "test-device-789"
        val approvalId = approvalService.createApproval(deviceId).block()!!
        val token = createAuthToken()

        webTestClient.post()
            .uri("/authorize/qr")
            .cookie("jwt", token)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("approval_id=$approvalId&new_role=INVALID_ROLE")
            .exchange()
            .expectStatus().isBadRequest
            .expectBody()
            .jsonPath("$.status").isEqualTo("error")
            .jsonPath("$.message").isEqualTo("Invalid role")
    }

    @Test
    fun `POST authorize qr should require authentication`() {
        val deviceId = "test-device-noauth"
        val approvalId = approvalService.createApproval(deviceId).block()!!

        webTestClient.post()
            .uri("/authorize/qr")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("approval_id=$approvalId&new_role=QUEUE_DISPLAY")
            .exchange()
            .expectStatus().is3xxRedirection
            .expectHeader().valueEquals("Location", "/login")
    }

    @Test
    fun `POST authorize qr should work with QUEUE_TAKING_QR_TARGET role`() {
        val deviceId = "test-device-qr-target"
        val approvalId = approvalService.createApproval(deviceId).block()!!
        val token = createAuthToken()

        webTestClient.post()
            .uri("/authorize/qr")
            .cookie("jwt", token)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("approval_id=$approvalId&new_role=QUEUE_TAKING_QR_TARGET")
            .exchange()
            .expectStatus().isOk
            .expectBody()
            .jsonPath("$.status").isEqualTo("success")
    }
}
