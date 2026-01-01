package app.qur.security

import org.springframework.http.HttpCookie
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
class JwtOAuth2SuccessHandler(
    private val jwtService: JwtService
) : ServerAuthenticationSuccessHandler {

    override fun onAuthenticationSuccess(
        webFilterExchange: WebFilterExchange,
        authentication: Authentication
    ): Mono<Void> {
        val oidcUser = authentication.principal as OidcUser
        val token = jwtService.generateToken(oidcUser)

        // Set JWT as HTTP-only cookie
        val cookie = ResponseCookie.from("jwt", token)
            .httpOnly(true)
            .secure(false) // Set to true in production with HTTPS
            .path("/")
            .maxAge(Duration.ofHours(24))
            .sameSite("Lax")
            .build()

        webFilterExchange.exchange.response.addCookie(cookie)
        webFilterExchange.exchange.response.statusCode = org.springframework.http.HttpStatus.FOUND
        webFilterExchange.exchange.response.headers.location = URI.create("/dashboard")

        return webFilterExchange.exchange.response.setComplete()
    }
}
