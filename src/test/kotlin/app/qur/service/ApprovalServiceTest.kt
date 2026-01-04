package app.qur.service

import app.qur.db.TestContainersConfig
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import reactor.test.StepVerifier
import java.time.Duration

@SpringBootTest
@Import(TestContainersConfig::class)
class ApprovalServiceTest {

    @Autowired
    private lateinit var approvalService: ApprovalService

    @Autowired
    private lateinit var redisTemplate: ReactiveStringRedisTemplate

    @BeforeEach
    fun cleanUp() {
        // Clean up any leftover test data
        redisTemplate.execute { connection ->
            connection.serverCommands().flushDb()
        }.blockLast()
    }

    @Test
    fun `createApproval should store device ID in Redis`() {
        val deviceId = "test-device-123"

        StepVerifier.create(
            approvalService.createApproval(deviceId)
                .flatMap { approvalId ->
                    // Verify device key exists
                    redisTemplate.opsForValue().get("approval:$approvalId:device")
                        .map { it to approvalId }
                }
        )
            .expectNextMatches { (storedDeviceId, _) ->
                storedDeviceId == deviceId
            }
            .verifyComplete()
    }

    @Test
    fun `waitForApproval should return immediately if already approved`() {
        val deviceId = "test-device-456"

        StepVerifier.create(
            approvalService.createApproval(deviceId)
                .flatMap { approvalId ->
                    // Approve first
                    approvalService.approve(approvalId, DeviceRole.QUEUE_DISPLAY)
                        .thenReturn(approvalId)
                }
                .flatMap { approvalId ->
                    // Wait for approval should return immediately
                    approvalService.waitForApproval(approvalId)
                }
        )
            .expectNextMatches { result ->
                result.deviceId == deviceId && result.newRole == DeviceRole.QUEUE_DISPLAY
            }
            .verifyComplete()
    }

    @Test
    fun `waitForApproval should wait and return ApprovalResult when approved via pub sub`() {
        val deviceId = "test-device-789"

        val waitMono = approvalService.createApproval(deviceId)
            .flatMap { approvalId ->
                // Start waiting in background, then approve after a delay
                approvalService.waitForApproval(approvalId)
                    .doFirst {
                        // Approve after a short delay
                        Thread {
                            Thread.sleep(1000)
                            approvalService.approve(approvalId, DeviceRole.QUEUE_TAKING_QR_TARGET)
                                .subscribe()
                        }.start()
                    }
            }

        StepVerifier.create(waitMono)
            .expectNextMatches { result ->
                result.deviceId == deviceId && result.newRole == DeviceRole.QUEUE_TAKING_QR_TARGET
            }
            .verifyComplete()
    }

    @Test
    fun `waitForApproval should return empty Mono on timeout`() {
        val deviceId = "test-device-timeout"

        StepVerifier.create(
            approvalService.createApproval(deviceId)
                .flatMap { approvalId ->
                    approvalService.waitForApproval(approvalId)
                }
        )
            .expectComplete()
            .verify(Duration.ofSeconds(35))
    }

    @Test
    fun `approve should set role publish to channel and return true`() {
        val deviceId = "test-device-approve"

        StepVerifier.create(
            approvalService.createApproval(deviceId)
                .flatMap { approvalId ->
                    approvalService.approve(approvalId, DeviceRole.QUEUE_DISPLAY)
                        .flatMap { success ->
                            // Verify role key was set
                            redisTemplate.opsForValue().get("approval:$approvalId:role")
                                .map { roleValue -> success to roleValue }
                        }
                }
        )
            .expectNextMatches { (success, roleValue) ->
                success && roleValue == "QUEUE_DISPLAY"
            }
            .verifyComplete()
    }

    @Test
    fun `approve should return false for non-existent approval_id`() {
        StepVerifier.create(
            approvalService.approve("non-existent-approval-id", DeviceRole.QUEUE_DISPLAY)
        )
            .expectNext(false)
            .verifyComplete()
    }

    @Test
    fun `cleanup should remove keys`() {
        val deviceId = "test-device-cleanup"

        StepVerifier.create(
            approvalService.createApproval(deviceId)
                .flatMap { approvalId ->
                    approvalService.approve(approvalId, DeviceRole.QUEUE_DISPLAY)
                        .thenReturn(approvalId)
                }
                .flatMap { approvalId ->
                    approvalService.cleanup(approvalId)
                        .thenReturn(approvalId)
                }
                .flatMap { approvalId ->
                    // Verify keys are deleted
                    redisTemplate.opsForValue().get("approval:$approvalId:device")
                        .defaultIfEmpty("deleted")
                        .zipWith(
                            redisTemplate.opsForValue().get("approval:$approvalId:role")
                                .defaultIfEmpty("deleted")
                        )
                }
        )
            .expectNextMatches { tuple ->
                tuple.t1 == "deleted" && tuple.t2 == "deleted"
            }
            .verifyComplete()
    }
}
