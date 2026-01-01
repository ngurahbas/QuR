package app.qur.test

import app.qur.db.TestContainersConfig
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import

@Configuration
@Import(
    TestContainersConfig::class,
    OAuth2MockServer::class,
    OAuth2MockEndpoints::class,
    TestOAuth2Config::class
)
class IntegrationTestConfig
