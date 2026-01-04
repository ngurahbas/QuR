package app.qur.web

import app.qur.service.ApprovalService
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
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
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

    @Autowired
    private lateinit var approvalService: ApprovalService

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

    @Test
    fun `GET qr setup should return QR code page for SETUP device`() {
        val setupDevice = Device(
            deviceId = "test-setup-device",
            deviceRole = DeviceRole.SETUP,
            expiredAt = LocalDateTime.now().plusHours(12)
        )
        val cookieValue = deviceService.encryptAndSerialize(setupDevice)

        val body = webTestClient.get()
            .uri("/qr/setup")
            .cookie("device", cookieValue)
            .exchange()
            .expectStatus().isOk
            .returnResult(String::class.java)
            .responseBody
            .collectList()
            .block()
            ?.joinToString("") ?: ""

        assert(body.contains("Device Setup")) { "Page should contain 'Device Setup' title" }
        assert(body.contains("qr-container")) { "Page should contain QR container" }
        assert(body.contains("hx-get")) { "Page should have HTMX polling" }
        assert(body.contains("/qr/setup/check/")) { "Page should poll the check endpoint" }
    }

    @Test
    fun `GET qr setup should redirect for non-SETUP device`() {
        val queueDevice = Device(
            deviceId = "test-queue-device",
            deviceRole = DeviceRole.QUEUE_DISPLAY,
            expiredAt = LocalDateTime.now().plusHours(12)
        )
        val cookieValue = deviceService.encryptAndSerialize(queueDevice)

        webTestClient.get()
            .uri("/qr/setup")
            .cookie("device", cookieValue)
            .exchange()
            .expectStatus().is3xxRedirection
    }

    @Test
    fun `GET qr setup should redirect for missing device cookie`() {
        webTestClient.get()
            .uri("/qr/setup")
            .exchange()
            .expectStatus().is3xxRedirection
    }

    @Test
    fun `GET qr setup check should return 204 on timeout`() {
        val approvalId = approvalService.createApproval("test-device-timeout").block()!!

        webTestClient.get()
            .uri("/qr/setup/check/$approvalId")
            .exchange()
            .expectStatus().isNoContent
    }

    @Test
    fun `GET qr setup check should return HX-Redirect on approval with QUEUE_DISPLAY`() {
        val deviceId = "test-device-redirect-display"
        val approvalId = approvalService.createApproval(deviceId).block()!!

        // Approve in background after short delay
        Thread {
            Thread.sleep(1000)
            approvalService.approve(approvalId, DeviceRole.QUEUE_DISPLAY).subscribe()
        }.start()

        val result = webTestClient.get()
            .uri("/qr/setup/check/$approvalId")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("HX-Redirect", "/qr/queue-display")
            .expectCookie().exists("device")
            .returnResult(Void::class.java)

        // Verify device cookie was updated
        val cookies = result.responseCookies
        val deviceCookie = cookies.getFirst("device")
        assert(deviceCookie != null) { "Device cookie should be set" }
        
        val device = deviceService.deserializeAndDecrypt(deviceCookie!!.value)
        assert(device.deviceRole == DeviceRole.QUEUE_DISPLAY) { "Device role should be QUEUE_DISPLAY" }
        assert(device.deviceId == deviceId) { "Device ID should match" }
    }

    @Test
    fun `GET qr setup check should return HX-Redirect on approval with QUEUE_TAKING_QR_TARGET`() {
        val deviceId = "test-device-redirect-qr-target"
        val approvalId = approvalService.createApproval(deviceId).block()!!

        // Approve in background after short delay
        Thread {
            Thread.sleep(1000)
            approvalService.approve(approvalId, DeviceRole.QUEUE_TAKING_QR_TARGET).subscribe()
        }.start()

        val result = webTestClient.get()
            .uri("/qr/setup/check/$approvalId")
            .exchange()
            .expectStatus().isOk
            .expectHeader().valueEquals("HX-Redirect", "/qr/queue-taking")
            .expectCookie().exists("device")
            .returnResult(Void::class.java)

        // Verify device cookie was updated
        val cookies = result.responseCookies
        val deviceCookie = cookies.getFirst("device")
        assert(deviceCookie != null) { "Device cookie should be set" }
        
        val device = deviceService.deserializeAndDecrypt(deviceCookie!!.value)
        assert(device.deviceRole == DeviceRole.QUEUE_TAKING_QR_TARGET) { "Device role should be QUEUE_TAKING_QR_TARGET" }
        assert(device.deviceId == deviceId) { "Device ID should match" }
    }

    @Test
    fun `GET qr setup check should return 404 for invalid approval_id`() {
        webTestClient.get()
            .uri("/qr/setup/check/invalid-approval-id-xyz")
            .exchange()
            .expectStatus().isNotFound
    }

    @Test
    fun `qr setup endpoints should be publicly accessible without authentication`() {
        val setupDevice = Device(
            deviceId = "test-public-device",
            deviceRole = DeviceRole.SETUP,
            expiredAt = LocalDateTime.now().plusHours(12)
        )
        val cookieValue = deviceService.encryptAndSerialize(setupDevice)

        // /qr/setup should be accessible
        webTestClient.get()
            .uri("/qr/setup")
            .cookie("device", cookieValue)
            .exchange()
            .expectStatus().isOk

        // /qr/setup/check should be accessible
        val approvalId = approvalService.createApproval("test-public-check").block()!!
        webTestClient.get()
            .uri("/qr/setup/check/$approvalId")
            .exchange()
            .expectStatus().isNoContent
    }
}
