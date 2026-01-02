package app.qur.security

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class JwtAuthenticationWebFilter(
    private val jwtService: JwtService
) : WebFilter {

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val token = exchange.request.cookies["jwt"]?.firstOrNull()?.value
        if (token != null && jwtService.validateToken(token)) {
            val claims = jwtService.extractClaims(token)
            val email = claims.subject
            val name = claims["name"] as? String

            val principal = JwtUserPrincipal(email, name, claims)
            val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
            val authentication = UsernamePasswordAuthenticationToken(principal, null, authorities)

            return chain.filter(exchange)
                .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
        }

        return chain.filter(exchange)
    }
}

data class JwtUserPrincipal(
    val email: String,
    val name: String?,
    val claims: Map<String, Any>
)
