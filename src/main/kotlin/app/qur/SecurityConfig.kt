package app.qur

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
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
                oauth2.loginPage("/login") //TODO: somehow spring boot standard /login inconflict with app.qur.web.AuthController.login
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

                                    object : Authentication {
                                        var _authenticated = true

                                        override fun isAuthenticated() = _authenticated

                                        override fun setAuthenticated(isAuthenticated: Boolean) {
                                            _authenticated = isAuthenticated
                                        }

                                        override fun getName() = null

                                        override fun getPrincipal() = EmailPrincipal(email)

                                        override fun getAuthorities() = emptyList<GrantedAuthority>()

                                        override fun getCredentials() = null

                                        override fun getDetails() = null

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