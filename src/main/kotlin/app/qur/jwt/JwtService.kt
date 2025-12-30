package app.qur.jwt

import org.slf4j.LoggerFactory
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class JwtService(
    private val jwtEncoder: JwtEncoder,
    private val jwtProperties: JwtProperties
) {
    private val logger = LoggerFactory.getLogger(JwtService::class.java)

    fun generateToken(oidcUser: OidcUser): String {
        val now = Instant.now()
        val expiration = now.plusMillis(jwtProperties.expirationMs)

        val subject = oidcUser.subject
        val email = oidcUser.email ?: oidcUser.getAttribute<String>("email") ?: "unknown"
        val name = oidcUser.fullName ?: oidcUser.getAttribute<String>("name") ?: "unknown"

        logger.info("Generating JWT for user - subject: {}, email: {}, name: {}", subject, email, name)

        val claims = JwtClaimsSet.builder()
            .issuer(jwtProperties.issuer)
            .issuedAt(now)
            .expiresAt(expiration)
            .subject(subject)
            .claim("email", email)
            .claim("name", name)
            .build()

        logger.debug("JWT claims created: {}", claims)

        val jwsHeader = JwsHeader.with(MacAlgorithm.HS256).build()
        val encoderParameters = JwtEncoderParameters.from(jwsHeader, claims)

        val jwt = jwtEncoder.encode(encoderParameters)

        logger.info("JWT generated successfully for subject: {}, expires at: {}", subject, expiration)
        logger.debug("JWT token value: {}", jwt.tokenValue)

        return jwt.tokenValue
    }
}
