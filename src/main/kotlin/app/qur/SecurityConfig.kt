package app.qur

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
class SecurityConfig {
    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/", "/error").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login { }
            .oauth2Client { }
            .csrf { csrf -> csrf.disable() }
            .build()
    }
}
