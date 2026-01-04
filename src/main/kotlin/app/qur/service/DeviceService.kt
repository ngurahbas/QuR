package app.qur.service

import app.qur.Secrets
import org.springframework.stereotype.Service
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Service
class DeviceService(
    private val secrets: Secrets
) {
    private val algorithm = "AES/GCM/NoPadding"
    private val gcmTagLength = 128
    private val gcmIvLength = 12
    private val delimiter = "|"

    fun encryptAndSerialize(device: Device): String {
        val plaintext = "${device.deviceId}$delimiter${device.deviceRole.name}"
        val keyBytes = deriveKey(secrets.device)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val iv = ByteArray(gcmIvLength)
        SecureRandom().nextBytes(iv)

        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(gcmTagLength, iv))

        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))

        val combined = iv + encryptedBytes
        return Base64.getEncoder().encodeToString(combined)
    }

    fun deserializeAndDecrypt(encrypted: String): Device {
        val combined = Base64.getDecoder().decode(encrypted)

        val iv = combined.copyOfRange(0, gcmIvLength)
        val ciphertext = combined.copyOfRange(gcmIvLength, combined.size)

        val keyBytes = deriveKey(secrets.device)
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance(algorithm)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(gcmTagLength, iv))

        val decryptedBytes = cipher.doFinal(ciphertext)
        val plaintext = String(decryptedBytes, Charsets.UTF_8)

        val parts = plaintext.split(delimiter)
        return Device(deviceId = parts[0], deviceRole = DeviceRole.valueOf(parts[1]))
    }

    private fun deriveKey(key: String): ByteArray {
        val digest = MessageDigest.getInstance("SHA-256")
        return digest.digest(key.toByteArray(Charsets.UTF_8))
    }
}