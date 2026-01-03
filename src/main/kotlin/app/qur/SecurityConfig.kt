package app.qur

import app.qur.security.JwtAuthenticationWebFilter
import app.qur.security.JwtOAuth2SuccessHandler
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
class SecurityConfig(
    private val jwtAuthenticationWebFilter: JwtAuthenticationWebFilter,
    private val jwtOAuth2SuccessHandler: JwtOAuth2SuccessHandler
) {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/", "/login", "/logins", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.authenticationSuccessHandler(jwtOAuth2SuccessHandler)
            }
            .addFilterAt(jwtAuthenticationWebFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            .csrf { csrf -> csrf.disable() }
            .build()
    }

    @Bean
    @ConfigurationProperties(prefix = "secrets")
    fun secrets(): MutableMap<String, String> {
        return mutableMapOf()
    }
}
