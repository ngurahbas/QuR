package app.qur.test

import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64
import java.util.Date
import java.util.UUID
import javax.crypto.SecretKey

@Configuration
class OAuth2MockServer {

    companion object {
        const val REALM = "qur"
        const val CLIENT_ID = "qur-client"
        const val CLIENT_SECRET = "tItZt1hOUxxNFFaeuvL35r0lQZva3et6"
        const val USERNAME = "user1"
        val SECRET_KEY = Keys.hmacShaKeyFor("changeme-minimum-32-characters-required-for-hs256-algorithm".toByteArray())
    }
}

@Controller
class OAuth2MockEndpoints {

    private val key = OAuth2MockServer.SECRET_KEY

    @GetMapping("/realms/qur/.well-known/openid-configuration")
    @ResponseBody
    fun discovery(request: ServerHttpRequest): Map<String, Any> {
        val baseUrl = getBaseUrl(request)
        return mapOf(
            "issuer" to "$baseUrl/realms/qur",
            "authorization_endpoint" to "$baseUrl/realms/qur/protocol/openid-connect/auth",
            "token_endpoint" to "$baseUrl/realms/qur/protocol/openid-connect/token",
            "userinfo_endpoint" to "$baseUrl/realms/qur/protocol/openid-connect/userinfo",
            "jwks_uri" to "$baseUrl/realms/qur/protocol/openid-connect/certs",
            "response_types_supported" to listOf("code", "token", "id_token"),
            "subject_types_supported" to listOf("public"),
            "id_token_signing_alg_values_supported" to listOf("HS256"),
            "grant_types_supported" to listOf("authorization_code", "refresh_token"),
            "token_endpoint_auth_methods_supported" to listOf("client_secret_basic", "client_secret_post"),
            "claims_supported" to listOf("sub", "name", "email", "preferred_username"),
            "scopes_supported" to listOf("openid", "profile", "email")
        )
    }

    @GetMapping("/realms/qur/protocol/openid-connect/auth")
    fun authorizationEndpoint(
        @RequestParam client_id: String,
        @RequestParam redirect_uri: String,
        @RequestParam response_type: String,
        @RequestParam state: String
    ): String {
        val authCode = UUID.randomUUID().toString()
        val callbackUrl = UriComponentsBuilder.fromUriString(redirect_uri)
            .queryParam("code", authCode)
            .queryParam("state", state)
            .build()
            .toUriString()
        return """
            <html>
            <body>
                <h2>Mock Keycloak Login</h2>
                <p>Simulating login for user: ${OAuth2MockServer.USERNAME}</p>
                <form method="post" action="$callbackUrl">
                    <input type="hidden" name="session_code" value="$authCode">
                    <button type="submit">Login as ${OAuth2MockServer.USERNAME}</button>
                </form>
            </body>
            </html>
        """.trimIndent()
    }

    @PostMapping("/realms/qur/protocol/openid-connect/token")
    @ResponseBody
    fun tokenEndpoint(
        @RequestParam grant_type: String,
        @RequestParam code: String,
        @RequestParam client_id: String,
        @RequestParam client_secret: String?,
        @RequestParam redirect_uri: String?,
        request: ServerHttpRequest
    ): Map<String, Any> {
        val baseUrl = getBaseUrl(request)

        if (grant_type != "authorization_code") {
            return mapOf("error" to "unsupported_grant_type")
        }

        val now = Instant.now()
        val accessToken = generateJwt(
            sub = OAuth2MockServer.USERNAME,
            iss = "$baseUrl/realms/qur",
            aud = OAuth2MockServer.CLIENT_ID,
            email = "${OAuth2MockServer.USERNAME}@example.com",
            name = "User One",
            preferredUsername = OAuth2MockServer.USERNAME,
            now = now,
            expiryHours = 1
        )

        val idToken = generateJwt(
            sub = OAuth2MockServer.USERNAME,
            iss = "$baseUrl/realms/qur",
            aud = OAuth2MockServer.CLIENT_ID,
            email = "${OAuth2MockServer.USERNAME}@example.com",
            name = "User One",
            preferredUsername = OAuth2MockServer.USERNAME,
            now = now,
            expiryHours = 1,
            nonce = "mock-nonce"
        )

        return mapOf(
            "access_token" to accessToken,
            "token_type" to "Bearer",
            "expires_in" to 3600,
            "refresh_token" to "mock-refresh-token",
            "id_token" to idToken
        )
    }

    @GetMapping("/realms/qur/protocol/openid-connect/userinfo")
    @ResponseBody
    fun userInfoEndpoint(@RequestParam(required = false) access_token: String?): Map<String, Any> {
        return mapOf(
            "sub" to OAuth2MockServer.USERNAME,
            "preferred_username" to OAuth2MockServer.USERNAME,
            "name" to "User One",
            "email" to "${OAuth2MockServer.USERNAME}@example.com",
            "email_verified" to true
        )
    }

    @GetMapping("/realms/qur/protocol/openid-connect/certs")
    @ResponseBody
    fun jwksEndpoint(): Map<String, Any> {
        val keyBytes = key.encoded
        val base64Key = Base64.getEncoder().encodeToString(keyBytes)
        return mapOf(
            "keys" to listOf(
                mapOf(
                    "kty" to "oct",
                    "alg" to "HS256",
                    "use" to "sig",
                    "k" to base64Key
                )
            )
        )
    }

    private fun generateJwt(
        sub: String,
        iss: String,
        aud: String,
        email: String,
        name: String,
        preferredUsername: String,
        now: Instant,
        expiryHours: Long,
        nonce: String? = null
    ): String {
        val builder = Jwts.builder()
            .id(UUID.randomUUID().toString())
            .subject(sub)
            .issuer(iss)
            .audience().add(aud).and()
            .issuedAt(Date.from(now))
            .expiration(Date.from(now.plus(expiryHours, ChronoUnit.HOURS)))
            .claim("email", email)
            .claim("name", name)
            .claim("preferred_username", preferredUsername)

        if (nonce != null) {
            builder.claim("nonce", nonce)
        }

        return builder.signWith(key).compact()
    }

    private fun getBaseUrl(request: ServerHttpRequest): String {
        val uri = request.uri
        val scheme = uri.scheme
        val host = uri.host
        val port = uri.port
        return if (port != null && port > 0 && port != 80 && port != 443) {
            "$scheme://$host:$port"
        } else {
            "$scheme://$host"
        }
    }
}
