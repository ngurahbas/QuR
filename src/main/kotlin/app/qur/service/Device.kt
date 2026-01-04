package app.qur.service

import java.time.LocalDateTime

data class Device(val deviceId: String, var deviceRole: DeviceRole, var expiredAt: LocalDateTime)