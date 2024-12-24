package com.kogo.content.endpoint.common

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.databind.SerializationFeature
import java.time.Instant
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class PageToken(
    val cursors: Map<String, CursorValue> = emptyMap(),
    val sortFields: List<SortField> = emptyList(),
    val filterFields: List<FilterField> = emptyList()
) {
    companion object {
        private val mapper = jacksonObjectMapper().apply {
            // Register Java 8 date/time module
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }

        fun create() = PageToken()

        fun fromString(token: String): PageToken = try {
            val decoded = Base64.getDecoder().decode(token)
            mapper.readValue(decoded, PageToken::class.java)
        } catch (e: Exception) {
            println("Failed to decode token: ${e.message}")
            create()
        }

        fun fromRequest(
            sortBy: Map<String, String> = emptyMap(),
            filterBy: Map<String, String> = emptyMap()
        ): PageToken {
            val sortFields = sortBy.map { (field, direction) ->
                SortField(
                    field = field,
                    direction = SortDirection.valueOf(direction.uppercase())
                )
            }

            val filters = filterBy.map { (field, value) ->
                FilterField(
                    field = field,
                    value = value,
                    operator = FilterOperator.EQUALS
                )
            }

            return PageToken(
                sortFields = sortFields,
                filterFields = filters
            )
        }
    }

    fun encode(): String {
        val json = mapper.writeValueAsString(this)
        return Base64.getEncoder().encodeToString(json.toByteArray())
    }

    fun nextPageToken(cursors: Map<String, CursorValue>): PageToken {
        return copy(cursors = cursors)
    }

    override fun toString(): String = encode()
}

data class CursorValue(
    val value: Any,
    val type: CursorValueType
) {
    fun toTypedValue(): Any = when (type) {
        CursorValueType.DATE -> when (value) {
            is java.time.Instant -> value
            is String -> java.time.Instant.parse(value)
            else -> throw IllegalArgumentException("Invalid date value type: ${value::class}")
        }
        else -> value
    }

    companion object {
        fun from(value: Any): CursorValue = CursorValue(
            value = when (value) {
                is Date -> value.toInstant()
                else -> value
            },
            type = when (value) {
                is String -> CursorValueType.STRING
                is Number -> CursorValueType.NUMBER
                is Date, is Instant -> CursorValueType.DATE
                else -> throw IllegalArgumentException("Unsupported cursor value type: ${value::class}")
            }
        )
    }
}

data class SortField(
    val field: String,
    val direction: SortDirection = SortDirection.DESC
)

data class FilterField(
    val field: String,
    val value: Any,
    val operator: FilterOperator
)

enum class SortDirection {
    ASC, DESC
}

enum class CursorValueType {
    STRING, NUMBER, DATE
}

enum class FilterOperator {
    EQUALS, IN, LESS_THAN, GREATER_THAN
}

// Simplified extension function
fun FilterOperator.toMongoOperator(): String = when (this) {
    FilterOperator.EQUALS -> "\$eq"
    FilterOperator.IN -> "\$in"
    FilterOperator.LESS_THAN -> "\$lt"
    FilterOperator.GREATER_THAN -> "\$gt"
}
