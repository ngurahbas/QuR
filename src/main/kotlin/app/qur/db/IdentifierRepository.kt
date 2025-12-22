package app.qur.db

import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.reactive.ReactiveCrudRepository
import org.springframework.stereotype.Repository
import reactor.core.publisher.Mono

@Repository
interface IdentifierRepository : ReactiveCrudRepository<Identifier, Long> {

    @Query("insert into identifier (type, value) values (:type, :value) on conflict (type, value) do update set updated_at = now() returning id")
    fun upsert(type: IdentifierType, value: String): Mono<Long>
}