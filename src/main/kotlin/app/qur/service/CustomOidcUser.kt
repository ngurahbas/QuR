package app.qur.service

import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.oidc.user.OidcUser

data class CustomOidcUser(private val _email: String): OidcUser {
    override fun getClaims() = mapOf<String, Any>()

    override fun getUserInfo() = null

    override fun getIdToken() = null

    override fun getAttributes() = mapOf<String, Any>()

    override fun getAuthorities() = emptyList<GrantedAuthority>()

    override fun getName() = _email
}