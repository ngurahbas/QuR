package app.qur.web

import org.springframework.core.env.ConfigurableEnvironment
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
    private val environment: ConfigurableEnvironment
) {

    @GetMapping("/logins")
    fun login(): Mono<Rendering> {
        val registrationIds = getOAuth2RegistrationIds()
        
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
            .map { providers ->
                Rendering.view("logins")
                    .modelAttribute("providers", providers)
                    .build()
            }
    }

    private fun getOAuth2RegistrationIds(): List<String> {
        val registrationIds = mutableSetOf<String>()
        val prefix = "spring.security.oauth2.client.registration."
        
        for (propertySource in environment.propertySources) {
            val source = propertySource.source
            if (source is Map<*, *>) {
                for (key in source.keys) {
                    val keyStr = key.toString()
                    if (keyStr.startsWith(prefix)) {
                        val suffix = keyStr.substring(prefix.length)
                        val firstDot = suffix.indexOf('.')
                        if (firstDot > 0) {
                            registrationIds.add(suffix.substring(0, firstDot))
                        }
                    }
                }
            }
        }
        
        return registrationIds.toList()
    }
}