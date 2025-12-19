package app.qur.db

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.time.LocalDateTime

@Table("identifier")
data class Identifier(
    @Id val id: Long? = null,
    val type: IdentifierType,
    val value: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)