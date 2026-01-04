package app.qur.web

import org.springframework.boot.security.oauth2.client.autoconfigure.OAuth2ClientProperties
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.ui.Model
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

data class OAuth2Provider(val registrationId: String, val displayName: String, val url: String)

@Controller
class AuthController(
    private val clientRegistrationRepository: ReactiveClientRegistrationRepository,
    private val oauth2ClientProperties: OAuth2ClientProperties
) {

    @GetMapping("/logins")
    fun login(model: Model): Mono<String> {
        val registrationIds = oauth2ClientProperties.registration.keys
        val providers = Flux.fromIterable(registrationIds)
            .flatMap { id -> clientRegistrationRepository.findByRegistrationId(id).map { it.toOAuth2Provider() } }

        return providers.collectList()
            .doOnNext { model.addAttribute("providers", it) }
            .map { "logins" }
    }

    private fun ClientRegistration.toOAuth2Provider() = OAuth2Provider(
        registrationId = registrationId,
        displayName = registrationId.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
        url = "/oauth2/authorization/$registrationId"
    )
}