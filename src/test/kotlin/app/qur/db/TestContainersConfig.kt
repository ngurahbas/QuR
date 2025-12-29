package app.qur.db

import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Bean
import org.testcontainers.containers.PostgreSQLContainer

@TestConfiguration
class TestContainersConfig {

    companion object {
        private val logger: Logger = LoggerFactory.getLogger(TestContainersConfig::class.java)
    }

    @Bean
    @ServiceConnection
    fun postgresContainer(): PostgreSQLContainer<*> {
        val pgContainer = PostgreSQLContainer("postgres:15-alpine");

        val r2dbcUrl =
            "r2dbc:postgresql://${pgContainer.host}:${pgContainer.firstMappedPort}/${pgContainer.databaseName}"
        System.setProperty("spring.r2dbc.url", r2dbcUrl)
        System.setProperty("spring.r2dbc.username", pgContainer.username)
        System.setProperty("spring.r2dbc.password", pgContainer.password)

        logger.info("PostgreSQL TestContainer configured")
        logger.info("R2DBC URL: {}", r2dbcUrl)
        logger.info("Database Name: {}", pgContainer.databaseName)
        logger.info("Username: {}", pgContainer.username)
        logger.info("Host: {}", pgContainer.host)
        logger.info("Mapped Port: {}", pgContainer.firstMappedPort)

        return pgContainer
    }
}