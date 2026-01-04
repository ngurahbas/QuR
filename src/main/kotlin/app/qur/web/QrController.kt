package app.qur.web

import app.qur.service.ApprovalService
import app.qur.service.Device
import app.qur.service.DeviceRole
import app.qur.service.DeviceService
import qrcode.QRCode
import qrcode.render.QRCodeGraphics
import java.io.ByteArrayOutputStream
import java.util.Base64
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@RestController
class QrController(
    private val deviceService: DeviceService,
    private val approvalService: ApprovalService
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
                val newDevice =
                    Device(UUID.randomUUID().toString(), DeviceRole.SETUP, LocalDateTime.now().plusHours(24))
                
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

    @GetMapping("/qr/setup/check/{approvalId}")
    fun checkApproval(
        @PathVariable approvalId: String,
        exchange: ServerWebExchange
    ): Mono<ResponseEntity<Void>> {
        return approvalService.waitForApproval(approvalId)
            .flatMap { result ->
                // Update device cookie with new role
                val newDevice = Device(
                    result.deviceId,
                    result.newRole,
                    LocalDateTime.now().plusHours(24)
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
                
                // Determine redirect path based on role
                val redirectPath = when (result.newRole) {
                    DeviceRole.QUEUE_TAKING_QR_TARGET -> "/qr/queue-taking"
                    DeviceRole.QUEUE_DISPLAY -> "/qr/queue-display"
                    else -> "/qr"
                }
                
                // Cleanup Redis entries
                approvalService.cleanup(approvalId)
                    .then(Mono.just(
                        ResponseEntity.ok()
                            .header("HX-Redirect", redirectPath)
                            .build<Void>()
                    ))
            }
            .switchIfEmpty(
                // Timeout - return 204 No Content
                Mono.just(ResponseEntity.noContent().build())
            )
            .onErrorResume { _ ->
                // Error - return 404
                Mono.just(ResponseEntity.notFound().build())
            }
    }
}

@Controller
class QrSetupController(
    private val deviceService: DeviceService,
    private val approvalService: ApprovalService
) {
    companion object {
        private const val COOKIE_NAME = "device"
    }

    @GetMapping("/qr/setup")
    fun qrSetup(exchange: ServerWebExchange, model: Model): Mono<String> {
        return Mono.defer {
            val existingCookie = exchange.request.cookies.getFirst(COOKIE_NAME)
            
            if (existingCookie == null) {
                return@defer Mono.just("redirect:/qr")
            }
            
            try {
                val device = deviceService.deserializeAndDecrypt(existingCookie.value)
                
                // Verify device role is SETUP
                if (device.deviceRole != DeviceRole.SETUP) {
                    return@defer Mono.just("redirect:/qr")
                }
                
                // Create approval and generate QR code
                approvalService.createApproval(device.deviceId)
                    .map { approvalId ->
                        // Generate QR code as PNG and embed as base64 data URL
                        val qrCode = QRCode(approvalId)
                        
                        // Render to PNG bytes
                        val pngBytes = ByteArrayOutputStream().use { outputStream ->
                            qrCode.render().writeImage(outputStream)
                            outputStream.toByteArray()
                        }
                        
                        // Convert to base64 data URL
                        val base64Image = Base64.getEncoder().encodeToString(pngBytes)
                        val imageTag = """<img src="data:image/png;base64,$base64Image" alt="QR Code" class="w-full h-auto"/>"""
                        
                        model.addAttribute("qrCodeSvg", imageTag)
                        model.addAttribute("approvalId", approvalId)
                        "qr-setup"
                    }
            } catch (e: Exception) {
                Mono.just("redirect:/qr")
            }
        }
    }
}
