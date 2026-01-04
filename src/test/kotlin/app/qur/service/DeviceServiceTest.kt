package app.qur.service

import app.qur.Secrets
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class DeviceServiceTest {

    private val secrets = Secrets("test-jwt-secret", "test-encryption-key")
    private val service = DeviceService(secrets)

    @Test
    fun `encrypt and decrypt should recover original device`() {
        val device = Device("device-123", DeviceRole.SETUP)

        val encrypted = service.encryptAndSerialize(device)
        val decrypted = service.deserializeAndDecrypt(encrypted)

        assertEquals(device.deviceId, decrypted.deviceId)
        assertEquals(device.deviceRole, decrypted.deviceRole)
    }
}
