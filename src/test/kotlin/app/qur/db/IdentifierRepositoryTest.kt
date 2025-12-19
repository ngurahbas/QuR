package app.qur.db

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier

@SpringBootTest
@Testcontainers
class IdentifierRepositoryTest {

    companion object {
        @Container
        @ServiceConnection
        val postgresContainer = PostgreSQLContainer("postgres:15-alpine")
    }
    
    @Autowired
    lateinit var repository: IdentifierRepository

    @Test
    fun `insert should return generated ID and create record`() {
        repository.insert(IdentifierType.EMAIL, "test@example.com")
            .`as`(StepVerifier::create)
            .expectNextMatches { id -> id > 0 }
            .verifyComplete()
    }

    @Test
    fun `findById should retrieve inserted record`() {
        // Insert a record first
        val insertResult = repository.insert(IdentifierType.MOBILE, "+1234567890")
        
        // Then retrieve it and verify
        insertResult.flatMap { generatedId ->
            repository.findById(generatedId)
        }
        .`as`(StepVerifier::create)
        .expectNextMatches { identifier ->
            identifier.type == IdentifierType.MOBILE &&
            identifier.value == "+1234567890"
        }
        .verifyComplete()
    }

    @Test
    fun `findById should return empty for non-existent ID`() {
        repository.findById(99999L)
            .`as`(StepVerifier::create)
            .verifyComplete()
    }
}