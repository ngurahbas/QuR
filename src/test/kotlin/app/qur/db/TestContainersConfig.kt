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
        val pgContainer = PostgreSQLContainer("postgres:15-alpine")
        pgContainer.start()

        logger.info("PostgreSQL TestContainer configured and started")
        logger.info("Database Name: {}", pgContainer.databaseName)
        logger.info("Username: {}", pgContainer.username)
        logger.info("Host: {}", pgContainer.host)
        logger.info("Mapped Port: {}", pgContainer.firstMappedPort)

        return pgContainer
    }
}