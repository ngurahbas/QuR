package app.qur

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "secrets")
class Secrets (val jwt: String, val device: String)
