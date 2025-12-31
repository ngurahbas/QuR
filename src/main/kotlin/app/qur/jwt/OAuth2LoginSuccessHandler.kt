package app.qur.jwt

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseCookie
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.net.URI
import java.time.Duration

@Component
class OAuth2LoginSuccessHandler(
    private val jwtService: JwtService
) : ServerAuthenticationSuccessHandler {

    private val logger = LoggerFactory.getLogger(OAuth2LoginSuccessHandler::class.java)

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        val oidcUser = authentication.principal as OidcUser

        logger.info("OAuth2 login successful for user: {}", oidcUser.email ?: oidcUser.subject)
        logger.info("User subject: {}", oidcUser.subject)
        logger.info("User email: {}", oidcUser.email)
        logger.info("User name: {}", oidcUser.fullName)

        val jwt = jwtService.generateToken(oidcUser)

        logger.info("JWT generated, length: {} characters", jwt.length)
        logger.debug("JWT value: {}", jwt)

        val cookie = ResponseCookie.from("Authorization-Token", jwt)
            .httpOnly(true)
            .secure(false)
            .path("/")
            .maxAge(Duration.ofHours(1))
            .sameSite("Lax")
            .build()

        val response = webFilterExchange.exchange.response
        response.addCookie(cookie)

        logger.info("JWT cookie set successfully")

        val redirectUrl = URI.create("/dashboard")
        response.statusCode = org.springframework.http.HttpStatus.FOUND
        response.headers.location = redirectUrl

        logger.info("Redirecting to: {}", redirectUrl)

        return response.setComplete()
    }
}
