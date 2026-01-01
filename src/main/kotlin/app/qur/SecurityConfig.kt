package app.qur

import app.qur.jwt.JwtAuthenticationFilter
import app.qur.jwt.OAuth2LoginSuccessHandler
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.web.server.SecurityWebFilterChain
import java.net.URI

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun springSecurityFilterChain(
        http: ServerHttpSecurity,
        oauth2LoginSuccessHandler: OAuth2LoginSuccessHandler,
        jwtAuthenticationFilter: JwtAuthenticationFilter
    ): SecurityWebFilterChain {
        return http
            .addFilterBefore(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/", "/login", "/logins", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.authenticationSuccessHandler(oauth2LoginSuccessHandler)
            }
            .oauth2Client { }
            .csrf { csrf -> csrf.disable() }
            .exceptionHandling { exceptionHandling ->
                exceptionHandling.authenticationEntryPoint { exchange, exception ->
                    //TODO this unusual log initialization
                    val logger = org.slf4j.LoggerFactory.getLogger(SecurityConfig::class.java)
                    logger.error("Authentication failed: {}", exception.message)

                    val response = exchange.response
                    response.statusCode = HttpStatus.FOUND
                    response.headers.location = URI.create("/oauth2/authorization/keycloak")
                    response.setComplete()
                }
            }
            .build()
    }
}
