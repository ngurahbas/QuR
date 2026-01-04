package app.qur.web

import app.qur.service.Device
import app.qur.service.DeviceRole
import app.qur.service.DeviceService
import app.qur.test.IntegrationTestConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Import
import org.springframework.test.web.reactive.server.WebTestClient
import java.time.Duration
import java.time.LocalDateTime

@SpringBootTest
@Import(IntegrationTestConfig::class)
class QrControllerTest {

    @Autowired
    private lateinit var applicationContext: ApplicationContext

    @Autowired
    private lateinit var deviceService: DeviceService

    private lateinit var webTestClient: WebTestClient

    @BeforeEach
    fun setUp() {
        webTestClient = WebTestClient.bindToApplicationContext(applicationContext).build()
    }

    @Test
    fun `should create new device cookie when cookie is not set`() {
        val result = webTestClient.get()
            .uri("/qr")
            .exchange()
            .expectStatus().isOk
            .expectCookie().exists("device")
            .returnResult(Void::class.java)

        val cookies = result.responseCookies
        val deviceCookie = cookies.getFirst("device")

        assert(deviceCookie != null) { "Device cookie should be set" }
        assert(deviceCookie!!.value.isNotEmpty()) { "Device cookie value should not be empty" }
        
        // Verify we can decrypt the cookie and it has correct properties
        val device = deviceService.deserializeAndDecrypt(deviceCookie.value)
        assert(device.deviceRole == DeviceRole.SETUP) { "Device role should be SETUP" }
        assert(device.expiredAt.isAfter(LocalDateTime.now())) { "Device should not be expired" }
        assert(device.deviceId.isNotEmpty()) { "Device should have an ID" }
        
        // Verify cookie properties
        assert(deviceCookie.isHttpOnly) { "Cookie should be HttpOnly" }
        assert(deviceCookie.maxAge == Duration.ofHours(24)) { "Cookie max age should be 24 hours" }
        assert(deviceCookie.sameSite == "Lax") { "Cookie SameSite should be Lax" }
        assert(deviceCookie.path == "/") { "Cookie path should be /" }
    }

    @Test
    fun `should not set new cookie when uptodate cookie exists`() {
        // Create a valid, up-to-date device cookie
        val validDevice = Device(
            deviceId = "test-device-id",
            deviceRole = DeviceRole.SETUP,
            expiredAt = LocalDateTime.now().plusHours(12)
        )
        val cookieValue = deviceService.encryptAndSerialize(validDevice)

        val result = webTestClient.get()
            .uri("/qr")
            .cookie("device", cookieValue)
            .exchange()
            .expectStatus().isOk
            .returnResult(Void::class.java)

        val cookies = result.responseCookies
        val deviceCookie = cookies.getFirst("device")

        // Should not set a new cookie when existing cookie is valid
        assert(deviceCookie == null) { "Should not set new cookie when existing cookie is valid" }
    }

    @Test
    fun `should create new cookie when expired cookie exists`() {
        // Create an expired device cookie
        val expiredDevice = Device(
            deviceId = "expired-device-id",
            deviceRole = DeviceRole.SETUP,
            expiredAt = LocalDateTime.now().minusHours(1)
        )
        val cookieValue = deviceService.encryptAndSerialize(expiredDevice)

        val result = webTestClient.get()
            .uri("/qr")
            .cookie("device", cookieValue)
            .exchange()
            .expectStatus().isOk
            .expectCookie().exists("device")
            .returnResult(Void::class.java)

        val cookies = result.responseCookies
        val deviceCookie = cookies.getFirst("device")

        assert(deviceCookie != null) { "New device cookie should be set" }
        
        // Verify the new cookie has a different device with valid properties
        val newDevice = deviceService.deserializeAndDecrypt(deviceCookie!!.value)
        assert(newDevice.deviceId != expiredDevice.deviceId) { "New device should have different ID" }
        assert(newDevice.deviceRole == DeviceRole.SETUP) { "New device role should be SETUP" }
        assert(newDevice.expiredAt.isAfter(LocalDateTime.now())) { "New device should not be expired" }
        
        // Verify the expiry is approximately 24 hours from now (within 1 minute tolerance)
        val expectedExpiry = LocalDateTime.now().plusHours(24)
        val timeDiff = java.time.Duration.between(newDevice.expiredAt, expectedExpiry).abs()
        assert(timeDiff.toMinutes() < 1) { "New device expiry should be approximately 24 hours from now" }
    }

    @Test
    fun `should create new cookie when invalid or corrupted cookie exists`() {
        val result = webTestClient.get()
            .uri("/qr")
            .cookie("device", "invalid-corrupted-cookie-value")
            .exchange()
            .expectStatus().isOk
            .expectCookie().exists("device")
            .returnResult(Void::class.java)

        val cookies = result.responseCookies
        val deviceCookie = cookies.getFirst("device")

        assert(deviceCookie != null) { "New device cookie should be set when decryption fails" }
        
        // Verify the new cookie is valid
        val device = deviceService.deserializeAndDecrypt(deviceCookie!!.value)
        assert(device.deviceRole == DeviceRole.SETUP) { "Device role should be SETUP" }
        assert(device.expiredAt.isAfter(LocalDateTime.now())) { "Device should not be expired" }
    }

    @Test
    fun `should be publicly accessible without authentication`() {
        // This test verifies that /qr endpoint does not require authentication
        webTestClient.get()
            .uri("/qr")
            .exchange()
            .expectStatus().isOk
            .expectCookie().exists("device")
    }
}
