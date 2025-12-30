package app.qur.jwt

import com.nimbusds.jose.jwk.source.ImmutableSecret
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import javax.crypto.spec.SecretKeySpec

@Configuration
@EnableConfigurationProperties(JwtProperties::class)
class JwtSecurityConfig(
    private val jwtProperties: JwtProperties
) {

    @Bean
    fun jwtEncoder(): JwtEncoder {
        val secret = SecretKeySpec(
            jwtProperties.secretKey.toByteArray(),
            "HmacSHA256"
        )
        return NimbusJwtEncoder(ImmutableSecret(secret))
    }

    @Bean
    fun jwtDecoder(): ReactiveJwtDecoder {
        val secret = SecretKeySpec(
            jwtProperties.secretKey.toByteArray(),
            "HmacSHA256"
        )
        return NimbusReactiveJwtDecoder.withSecretKey(secret)
            .macAlgorithm(MacAlgorithm.HS256)
            .build()
    }
}
