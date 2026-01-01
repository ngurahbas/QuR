package app.qur.test

import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Primary
import org.springframework.security.oauth2.client.registration.ClientRegistration
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository
import org.springframework.security.oauth2.core.AuthorizationGrantType
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames
import app.qur.web.OAuth2ClientProperties
import reactor.core.publisher.Mono

@TestConfiguration
class TestOAuth2Config {

    @Bean
    @Primary
    fun testClientRegistrationRepository(): ReactiveClientRegistrationRepository {
        val issuerUri = "http://localhost:8080/realms/qur"
        val registration = ClientRegistration.withRegistrationId("keycloak")
            .clientId(OAuth2MockServer.CLIENT_ID)
            .clientSecret(OAuth2MockServer.CLIENT_SECRET)
            .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
            .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
            .scope("openid", "profile", "email")
            .authorizationUri("$issuerUri/protocol/openid-connect/auth")
            .tokenUri("$issuerUri/protocol/openid-connect/token")
            .userInfoUri("$issuerUri/protocol/openid-connect/userinfo")
            .userNameAttributeName(IdTokenClaimNames.SUB)
            .jwkSetUri("$issuerUri/protocol/openid-connect/certs")
            .issuerUri(issuerUri)
            .build()

        return ReactiveClientRegistrationRepository { Mono.just(registration) }
    }

    @Bean
    @Primary
    fun testOAuth2ClientProperties(): OAuth2ClientProperties {
        return OAuth2ClientProperties(
            registration = mapOf(
                "keycloak" to OAuth2ClientProperties.Registration(
                    provider = "keycloak",
                    clientId = OAuth2MockServer.CLIENT_ID
                )
            )
        )
    }
}
