package app.qur.service

import org.springframework.data.redis.core.ReactiveStringRedisTemplate
import org.springframework.data.redis.listener.ChannelTopic
import org.springframework.data.redis.listener.ReactiveRedisMessageListenerContainer
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.time.Duration
import java.util.*

@Service
class ApprovalService(
    private val redisTemplate: ReactiveStringRedisTemplate,
    private val redisMessageListenerContainer: ReactiveRedisMessageListenerContainer
) {
    companion object {
        private const val KEY_PREFIX = "approval:"
        private const val CHANNEL_PREFIX = "approval:channel:"
        private val EXPIRATION = Duration.ofMinutes(5)
        private val POLL_TIMEOUT = Duration.ofSeconds(30)
    }

    /**
     * Creates an approval entry in Redis with the given device ID.
     * Returns the generated approval ID.
     */
    fun createApproval(deviceId: String): Mono<String> {
        val approvalId = UUID.randomUUID().toString()
        val deviceKey = "$KEY_PREFIX$approvalId:device"
        
        return redisTemplate.opsForValue()
            .set(deviceKey, deviceId, EXPIRATION)
            .thenReturn(approvalId)
    }

    /**
     * Waits for approval of the given approval ID.
     * First checks if already approved, otherwise subscribes to Redis pub/sub and waits up to 30 seconds.
     * Returns ApprovalResult on approval, empty Mono on timeout.
     */
    fun waitForApproval(approvalId: String): Mono<ApprovalResult> {
        val roleKey = "$KEY_PREFIX$approvalId:role"
        val deviceKey = "$KEY_PREFIX$approvalId:device"
        
        // First check if already approved
        return redisTemplate.opsForValue().get(roleKey)
            .flatMap { roleValue ->
                // Already approved, get device ID and return
                redisTemplate.opsForValue().get(deviceKey)
                    .map { deviceId ->
                        ApprovalResult(deviceId, DeviceRole.valueOf(roleValue))
                    }
            }
            .switchIfEmpty(
                // Not yet approved, subscribe to pub/sub channel and wait
                redisMessageListenerContainer
                    .receive(ChannelTopic("$CHANNEL_PREFIX$approvalId"))
                    .next()
                    .flatMap { message ->
                        val roleValue = message.message
                        // Get device ID from Redis
                        redisTemplate.opsForValue().get(deviceKey)
                            .map { deviceId ->
                                ApprovalResult(deviceId, DeviceRole.valueOf(roleValue))
                            }
                    }
                    .timeout(POLL_TIMEOUT, Mono.empty())
            )
    }

    /**
     * Approves the given approval ID with the specified new role.
     * Sets role in Redis and publishes to channel to notify waiting subscribers.
     * Returns false if approval_id doesn't exist.
     */
    fun approve(approvalId: String, newRole: DeviceRole): Mono<Boolean> {
        val roleKey = "$KEY_PREFIX$approvalId:role"
        val deviceKey = "$KEY_PREFIX$approvalId:device"
        val channel = "$CHANNEL_PREFIX$approvalId"
        
        // First check if device key exists
        return redisTemplate.opsForValue().get(deviceKey)
            .flatMap { _ ->
                // Device exists, set role and publish
                redisTemplate.opsForValue()
                    .set(roleKey, newRole.name, EXPIRATION)
                    .then(
                        redisTemplate.convertAndSend(channel, newRole.name)
                    )
                    .thenReturn(true)
            }
            .defaultIfEmpty(false)
    }

    /**
     * Cleanup approval keys after processing
     */
    fun cleanup(approvalId: String): Mono<Boolean> {
        val roleKey = "$KEY_PREFIX$approvalId:role"
        val deviceKey = "$KEY_PREFIX$approvalId:device"
        
        return redisTemplate.delete(roleKey, deviceKey)
            .map { count -> count > 0 }
    }
}

data class ApprovalResult(val deviceId: String, val newRole: DeviceRole)
