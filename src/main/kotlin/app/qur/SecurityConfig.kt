package app.qur

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.oauth2.client.authentication.OAuth2AuthorizationCodeAuthenticationToken
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.client.web.server.ServerOAuth2AuthorizationCodeAuthenticationTokenConverter
import org.springframework.security.oauth2.core.user.OAuth2User
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
                            return super.convert(exchange).map { authentication ->
                                val token = authentication as OAuth2AuthorizationCodeAuthenticationToken
                                val oauth2User = token.principal as? OAuth2User
                                
                                if (oauth2User == null) {
                                    authentication
                                } else {
                                    val email = oauth2User.getAttribute<String>("email") 
                                        ?: throw IllegalArgumentException("Email not found")
                                    
                                    val clientRegistration = token.clientRegistration
                                    val authorizationExchange = token.authorizationExchange
                                    val accessToken = token.accessToken
                                    val refreshToken = token.refreshToken
                                    val additionalParameters = token.additionalParameters
                                    
                                    object : OAuth2AuthorizationCodeAuthenticationToken(
                                        clientRegistration,
                                        authorizationExchange,
                                        accessToken,
                                        refreshToken,
                                        additionalParameters
                                    ) {
                                        override fun getPrincipal(): Any = EmailPrincipal(email)
                                    }
                                }
                            }
                        }
                    })
            }
            .oauth2Client { }
            .csrf { csrf -> csrf.disable() }
            .build()
    }
}

class EmailPrincipal(val email: String)