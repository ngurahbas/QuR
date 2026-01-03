package app.qur

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SecretsConfig {

    @Bean
    @ConfigurationProperties(prefix = "secrets")
    fun secrets(): MutableMap<String, String> {
        return mutableMapOf()
    }
}
