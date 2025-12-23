package app.qur

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationCodeAuthenticationTokenConverter
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

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
                oauth2.loginPage("/login")
                    .authenticationSuccessHandler(RedirectServerAuthenticationSuccessHandler("/hello"))
                    .authenticationConverter(object :
                        ServerOAuth2AuthorizationCodeAuthenticationTokenConverter(reactiveClientRegistrationRepository) {
                        override fun convert(exchange: ServerWebExchange): Mono<Authentication> {
                            return super.convert(exchange)
                        }
                    })
            }
            .oauth2Client { }
            .csrf { csrf -> csrf.disable() }
            .build()
    }
}
