package app.qur.web

import app.qur.security.JwtUserPrincipal
import app.qur.service.ApprovalService
import app.qur.service.DeviceRole
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Mono

@RestController
class AuthorizeController(
    private val approvalService: ApprovalService
) {
    @PostMapping("/authorize/qr")
    fun authorizeQr(
        @AuthenticationPrincipal principal: JwtUserPrincipal,
        @RequestParam("approval_id") approvalId: String,
        @RequestParam("new_role") newRole: String
    ): Mono<ResponseEntity<Map<String, String>>> {
        // Validate newRole is QUEUE_TAKING_QR_TARGET or QUEUE_DISPLAY
        val deviceRole = try {
            DeviceRole.valueOf(newRole)
        } catch (e: IllegalArgumentException) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .body(mapOf("status" to "error", "message" to "Invalid role"))
            )
        }
        
        if (deviceRole != DeviceRole.QUEUE_TAKING_QR_TARGET && deviceRole != DeviceRole.QUEUE_DISPLAY) {
            return Mono.just(
                ResponseEntity.badRequest()
                    .body(mapOf("status" to "error", "message" to "Role must be QUEUE_TAKING_QR_TARGET or QUEUE_DISPLAY"))
            )
        }
        
        return approvalService.approve(approvalId, deviceRole)
            .map { success ->
                if (success) {
                    ResponseEntity.ok(mapOf("status" to "success"))
                } else {
                    ResponseEntity.badRequest()
                        .body(mapOf("status" to "error", "message" to "Approval ID not found"))
                }
            }
    }
}
