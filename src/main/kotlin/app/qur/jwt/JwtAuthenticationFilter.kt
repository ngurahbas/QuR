package app.qur.jwt

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.BadJwtException
import org.springframework.security.oauth2.jwt.JwtValidationException
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.resource.NoResourceFoundException
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationFilter(
    private val jwtDecoder: ReactiveJwtDecoder
) : WebFilter {

    private val logger = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val cookie = exchange.request.cookies.getFirst("Authorization-Token")

        if (cookie == null) {
            logger.debug("No Authorization-Token cookie found")
            return chain.filter(exchange)
        }

        logger.info("JWT cookie found, attempting validation")
        logger.debug("Cookie value: {}...", cookie.value.take(50))

        return jwtDecoder.decode(cookie.value)
            .flatMap { jwt ->
                logger.info("JWT validated successfully for subject: {}", jwt.subject)
                logger.debug("JWT claims: {}", jwt.claims)

                val authentication = UsernamePasswordAuthenticationToken(
                    jwt.subject,
                    null,
                    listOf(SimpleGrantedAuthority("ROLE_USER"))
                )

                val context = exchange.mutate()
                    .principal(Mono.just(authentication))
                    .build()

                chain.filter(context)
            }
            .onErrorResume { e ->
                when (e) {
                    is BadJwtException,
                    is JwtValidationException -> {
                        logger.error("JWT validation failed: {}", e.message)
                        logger.debug("JWT validation error details", e)
                    }
                    is NoResourceFoundException -> {
                        logger.debug("Skipping JWT validation for static resource request: {}", e.message)
                    }
                    else -> {
                        logger.debug("Unexpected error during JWT processing: {} - {}", e.javaClass.simpleName, e.message)
                    }
                }
                chain.filter(exchange)
            }
    }
}
