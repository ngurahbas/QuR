package app.qur

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler

@Configuration
class SecurityConfig(
    private val reactiveClientRegistrationRepository: ReactiveClientRegistrationRepository
) {

    @Bean
    fun springSecurityFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/", "/login", "/error", "/css/**", "/js/**", "/images/**").permitAll()
                    .anyExchange().authenticated()
            }
            .oauth2Login { oauth2 ->
                oauth2.authenticationSuccessHandler(RedirectServerAuthenticationSuccessHandler("/hello"))
            }
            .oauth2Client { }
            .csrf { csrf -> csrf.disable() }
            .build()
    }
}

class EmailPrincipal(val email: String)