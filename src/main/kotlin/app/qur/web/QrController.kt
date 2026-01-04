package app.qur.web

import app.qur.service.Device
import app.qur.service.DeviceRole
import app.qur.service.DeviceService
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@RestController
class QrController(
    private val deviceService: DeviceService
) {
    companion object {
        private const val COOKIE_NAME = "device"
        private val COOKIE_MAX_AGE = Duration.ofHours(24)
    }

    @GetMapping("/qr")
    fun qr(exchange: ServerWebExchange): Mono<ResponseEntity<Void>> {
        return Mono.defer {
            val existingCookie = exchange.request.cookies.getFirst(COOKIE_NAME)
            
            val shouldSetCookie = if (existingCookie != null) {
                try {
                    val device = deviceService.deserializeAndDecrypt(existingCookie.value)
                    device.expiredAt.isBefore(LocalDateTime.now())
                } catch (e: Exception) {
                    true
                }
            } else {
                true
            }
            
            if (shouldSetCookie) {
                val newDevice = Device(
                    deviceId = UUID.randomUUID().toString(),
                    deviceRole = DeviceRole.SETUP,
                    expiredAt = LocalDateTime.now().plusHours(24)
                )
                
                val encryptedValue = deviceService.encryptAndSerialize(newDevice)
                
                val cookie = ResponseCookie.from(COOKIE_NAME, encryptedValue)
                    .httpOnly(true)
                    .secure(false) // TODO: Set to true in production
                    .path("/")
                    .maxAge(COOKIE_MAX_AGE)
                    .sameSite("Lax")
                    .build()
                
                exchange.response.addCookie(cookie)
            }
            
            Mono.just(ResponseEntity.ok().build())
        }
    }
}
