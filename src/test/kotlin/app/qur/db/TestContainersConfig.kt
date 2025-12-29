package app.qur.db

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.reactive.function.client.WebClient
import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.wait.strategy.Wait
import org.testcontainers.utility.DockerImageName
import reactor.core.publisher.Mono
import java.time.Duration
import org.testcontainers.containers.wait.strategy.WaitStrategy
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy

@TestConfiguration
class TestContainersConfig {

    private val logger: Logger = LoggerFactory.getLogger(TestContainersConfig::class.java)

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        val pgContainer = PostgreSQLContainer("postgres:18.1-alpine")
        pgContainer.start()

        logger.info("PostgreSQL TestContainer configured and started")
        logger.info("Database Name: {}", pgContainer.databaseName)
        logger.info("Username: {}", pgContainer.username)
        logger.info("Host: {}", pgContainer.host)
        logger.info("Mapped Port: {}", pgContainer.firstMappedPort)

        return pgContainer
    }



    @Bean
    fun keycloakContainer(): GenericContainer<*> {
        val container = GenericContainer(DockerImageName.parse("quay.io/keycloak/keycloak:25.0"))
            .withExposedPorts(8080)
            .withEnv("KEYCLOAK_ADMIN", "admin")
            .withEnv("KEYCLOAK_ADMIN_PASSWORD", "admin")
            .withEnv("KC_HTTP_PORT", "8080")
            .waitingFor(Wait.forHttp("/realms/master/.well-known/openid-configuration").forStatusCode(200).withStartupTimeout(Duration.ofSeconds(120)))
            .withCommand("start-dev")

        container.start()

        val keycloakUrl = "http://${container.host}:${container.getMappedPort(8080)}"
        val issuerUri = "$keycloakUrl/realms/$REALM"

        logger.info("Keycloak TestContainer started")
        logger.info("Keycloak URL: {}", keycloakUrl)
        logger.info("Issuer URI: {}", issuerUri)

        configureKeycloakRealm(keycloakUrl)

        System.setProperty("spring.security.oauth2.client.provider.keycloak.issuer-uri", issuerUri)
        System.setProperty("spring.security.oauth2.client.registration.keycloak.client-id", CLIENT_ID)
        System.setProperty("spring.security.oauth2.client.registration.keycloak.client-secret", CLIENT_SECRET)

        return container
    }

    private fun configureKeycloakRealm(keycloakUrl: String) {
        logger.info("Configuring Keycloak realm: {}", REALM)

        val webClient = WebClient.builder()
            .baseUrl(keycloakUrl)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build()

        val adminToken = getAdminToken(webClient)

        createRealm(webClient, adminToken)
        createClient(webClient, adminToken)
        createUser(webClient, adminToken)

        logger.info("Keycloak realm configuration complete")
    }

    private fun getAdminToken(webClient: WebClient): String {
        val response = webClient.post()
            .uri("/realms/master/protocol/openid-connect/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .bodyValue("username=admin&password=admin&grant_type=password&client_id=admin-cli")
            .retrieve()
            .bodyToMono(Map::class.java)
            .block()!!

        return response["access_token"] as String
    }

    private fun createRealm(webClient: WebClient, adminToken: String) {
        val existingRealm = webClient.get()
            .uri("/admin/realms/$REALM")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .exchangeToMono { resp -> Mono.just(resp.statusCode() == HttpStatus.OK) }
            .block()

        if (existingRealm != true) {
            webClient.post()
                .uri("/admin/realms")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .bodyValue(mapOf("realm" to REALM, "enabled" to true))
                .retrieve()
                .bodyToMono(Void::class.java)
                .block()
            logger.info("Created realm: {}", REALM)
        } else {
            logger.info("Realm already exists: {}", REALM)
        }
    }

    private fun createClient(webClient: WebClient, adminToken: String) {
        val clients = webClient.get()
            .uri("/admin/realms/$REALM/clients?clientId=$CLIENT_ID")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .retrieve()
            .bodyToMono(List::class.java)
            .block() as List<*>

        if (clients.isEmpty()) {
            webClient.post()
                .uri("/admin/realms/$REALM/clients")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .bodyValue(mapOf(
                    "clientId" to CLIENT_ID,
                    "enabled" to true,
                    "publicClient" to false,
                    "secret" to CLIENT_SECRET,
                    "standardFlowEnabled" to true,
                    "directAccessGrantsEnabled" to true,
                    "redirectUris" to listOf("http://localhost:8080/*"),
                    "webOrigins" to listOf("http://localhost:8080")
                ))
                .retrieve()
                .bodyToMono(Void::class.java)
                .block()
            logger.info("Created client: {}", CLIENT_ID)
        } else {
            logger.info("Client already exists: {}", CLIENT_ID)
        }
    }

    private fun createUser(webClient: WebClient, adminToken: String) {
        val users = webClient.get()
            .uri("/admin/realms/$REALM/users?username=$USERNAME")
            .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
            .retrieve()
            .bodyToMono(List::class.java)
            .block() as List<*>

        if (users.isEmpty()) {
            webClient.post()
                .uri("/admin/realms/$REALM/users")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $adminToken")
                .bodyValue(mapOf(
                    "username" to USERNAME,
                    "enabled" to true,
                    "firstName" to "User",
                    "lastName" to "One",
                    "email" to "$USERNAME@example.com",
                    "emailVerified" to true,
                    "credentials" to listOf(mapOf(
                        "type" to "password",
                        "value" to PASSWORD,
                        "temporary" to false
                    ))
                ))
                .retrieve()
                .bodyToMono(Void::class.java)
                .block()
            logger.info("Created user: {}", USERNAME)
        } else {
            logger.info("User already exists: {}", USERNAME)
        }
    }

    companion object {
        const val REALM = "qur"
        const val CLIENT_ID = "qur-client"
        const val CLIENT_SECRET = "tItZt1hOUxxNFFaeuvL35r0lQZva3et6"
        const val USERNAME = "user1"
        const val PASSWORD = "password1"
    }
}
