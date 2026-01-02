package app.qur.web

import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.reactive.result.view.Rendering
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class OAuth2Provider(val registrationId: String, val displayName: String, val url: String)

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
                clientRegistrationRepository.findByRegistrationId(registrationId).map { it.toOAuth2Provider() }
            }
            .collectList()
            .map { Rendering.view("logins").modelAttribute("providers", it).build() }
    }

    private fun ClientRegistration.toOAuth2Provider() = OAuth2Provider(
        registrationId = registrationId,
        displayName = registrationId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
        url = "/oauth2/authorization/$registrationId"
    )
}