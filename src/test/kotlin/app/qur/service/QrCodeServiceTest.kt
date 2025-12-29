package app.qur.service

import org.junit.jupiter.api.Test
import reactor.test.StepVerifier

class QrCodeServiceTest {

    private val service = QrCodeService()

    @Test
    fun `generatePng should return valid PNG bytes for valid input`() {
        service.generatePng("https://example.com")
            .`as`(StepVerifier::create)
            .expectNextMatches { bytes ->
                bytes.isNotEmpty() &&
                bytes.size > 100 &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte() &&
                bytes[2] == 0x4E.toByte() &&
                bytes[3] == 0x47.toByte()
            }
            .verifyComplete()
    }

    @Test
    fun `generatePng should wrap exceptions from library`() {
        service.generatePng("")
            .`as`(StepVerifier::create)
            .expectError(QrCodeGenerationException::class.java)
            .verify()
    }

    @Test
    fun `generatePng should handle unicode characters`() {
        service.generatePng("Hello 世界 🌍")
            .`as`(StepVerifier::create)
            .expectNextMatches { bytes ->
                bytes.isNotEmpty() &&
                bytes[0] == 0x89.toByte() &&
                bytes[1] == 0x50.toByte()
            }
            .verifyComplete()
    }
}
