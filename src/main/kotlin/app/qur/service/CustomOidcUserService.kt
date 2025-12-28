package app.qur.service

import org.springframework.security.oauth2.client.oidc.userinfo.OidcReactiveOAuth2UserService
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class CustomOidcUserService: OidcReactiveOAuth2UserService() {
    override fun loadUser(userRequest: OidcUserRequest?): Mono<OidcUser> {
        return super.loadUser(userRequest)
    }
}