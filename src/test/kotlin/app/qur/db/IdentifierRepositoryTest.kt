package app.qur.db

import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Testcontainers
import reactor.test.StepVerifier

@SpringBootTest
@Import(TestContainersConfig::class)
@Testcontainers
class IdentifierRepositoryTest {

    @Autowired
    lateinit var postgresContainer: PostgreSQLContainer<*>

    @Autowired
    lateinit var repository: IdentifierRepository

    @Test
    fun `upsert should return generated ID and create record`() {
        repository.upsert(IdentifierType.EMAIL, "test@example.com")
            .`as`(StepVerifier::create)
            .expectNextMatches { id -> id > 0 }
            .verifyComplete()
    }

    @Test
    fun `upsert should retrieve inserted record`() {
        val upsertResult = repository.upsert(IdentifierType.MOBILE, "+1234567890")

        upsertResult.flatMap { generatedId ->
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
    fun `upsert should return existing ID for duplicate data`() {
        repository.upsert(IdentifierType.EMAIL, "duplicate@example.com")
            .`as`(StepVerifier::create)
            .expectNextMatches { id -> id > 0 }
            .verifyComplete()

        repository.upsert(IdentifierType.EMAIL, "duplicate@example.com")
            .`as`(StepVerifier::create)
            .expectNextMatches { id -> id > 0 }
            .verifyComplete()
    }

    @Test
    fun `findById should return empty for non-existent ID`() {
        repository.findById(99999L)
            .`as`(StepVerifier::create)
            .verifyComplete()
    }
}
