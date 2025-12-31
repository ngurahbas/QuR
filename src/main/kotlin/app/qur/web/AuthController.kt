package app.qur.web

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.result.view.Rendering
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class OAuth2Provider(val registrationId: String, val displayName: String, val url: String)

@ConfigurationProperties(prefix = "spring.security.oauth2.client")
data class OAuth2ClientProperties(
    val registration: Map<String, Registration> = emptyMap()
) {
    data class Registration(
        val provider: String? = null,
        val clientId: String? = null
    )
}

@Controller
class AuthController(
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository,
    private val oauth2ClientProperties: OAuth2ClientProperties
) {

    @GetMapping("/logins")
    fun login(): Mono<Rendering> {
        val registrationIds = oauth2ClientProperties.registration.keys

        return Flux.fromIterable(registrationIds)
            .flatMap { registrationId ->
                clientRegistrationRepository.findByRegistrationId(registrationId)
                    .map { registration ->
                        OAuth2Provider(
                            registrationId = registration.registrationId,
                            displayName = registration.registrationId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                            url = "/oauth2/authorization/${registration.registrationId}"
                        )
                    }
            }
            .collectList()
            .map { providers -> Rendering.view("logins").modelAttribute("providers", providers).build() }
    }
}