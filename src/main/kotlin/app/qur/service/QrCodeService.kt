package app.qur.service

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.MultiFormatWriter
import com.google.zxing.client.j2se.MatrixToImageWriter
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.io.ByteArrayOutputStream

@Service
class QrCodeService {
    fun generatePng(data: String): Mono<ByteArray> = Mono.fromCallable {
        try {
            val hints = mapOf(
                EncodeHintType.CHARACTER_SET to "UTF-8",
                EncodeHintType.MARGIN to 1
            )
            val bitMatrix = MultiFormatWriter().encode(
                data,
                BarcodeFormat.QR_CODE,
                1024,
                1024,
                hints
            )
            val buffer = ByteArrayOutputStream()
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", buffer)
            buffer.toByteArray()
        } catch (e: Exception) {
            throw QrCodeGenerationException("Failed to generate QR code: ${e.message}", e)
        }
    }
}
