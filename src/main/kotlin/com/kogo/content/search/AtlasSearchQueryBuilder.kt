package com.kogo.content.search

import com.kogo.content.lib.*
import org.bson.Document
import org.springframework.data.domain.Sort
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.LimitOperation
import org.springframework.data.mongodb.core.aggregation.MatchOperation
import org.springframework.data.mongodb.core.aggregation.SkipOperation
import org.springframework.data.mongodb.core.aggregation.SortOperation
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AtlasSearchQueryBuilder(
    private val mongoTemplate: MongoTemplate
) {
    data class ScoreField(
        val field: String,
        val boost: Double? = null,
        val boostPath: String? = null
    )

    fun <T : Any> search(
        entityClass: KClass<T>,
        searchIndex: String,
        paginationRequest: PaginationRequest,
        searchText: String,
        searchFields: List<String>,
        scoreFields: List<ScoreField>? = null
    ): PaginationSlice<T> {
        val operations = mutableListOf<AggregationOperation>()

        operations.add(AggregationOperation {
            Document("\$search", Document().apply {
                put("index", searchIndex)
                
                // If we have score fields, use compound operator
                if (!scoreFields.isNullOrEmpty()) {
                    put("compound", Document().apply {
                        put("should", buildShouldClauses(searchText, searchFields, scoreFields))
                    })
                } else {
                    // Simple text search without scoring
                    put("text", Document().apply {
                        put("query", searchText)
                        put("path", searchFields)
                    })
                }
            })
        })

        // Add filter stages
        paginationRequest.pageToken.filters.forEach { filter ->
            operations.add(
                MatchOperation(
                    Criteria.where(filter.field).apply {
                        when (filter.operator) {
                            FilterOperator.EQUALS -> `is`(filter.value)
                            FilterOperator.IN -> `in`(filter.value)
                        }
                    }
                )
            )
        }

        // Add sort stages
        if (paginationRequest.pageToken.sortFields.isNotEmpty()) {
            val sortOperations = paginationRequest.pageToken.sortFields.map { sort ->
                Sort.Order(
                    if (sort.direction == SortDirection.ASC) Sort.Direction.ASC else Sort.Direction.DESC,
                    sort.field
                )
            }.toMutableList()

            // Add score sorting if using search
            sortOperations.add(Sort.Order(Sort.Direction.DESC, "score"))
            operations.add(SortOperation(Sort.by(sortOperations)))
        } else {
            // Default sort by score
            operations.add(SortOperation(Sort.by(Sort.Direction.DESC, "score")))
        }

        // Apply cursor-based pagination
        paginationRequest.pageToken.cursors["searchAfter"]?.let { cursor ->
            val skipValue = when (val value = cursor.value) {
                is Int -> value.toLong()
                is Long -> value
                else -> throw IllegalArgumentException("Invalid cursor value type: ${value::class}")
            }
            operations.add(SkipOperation(skipValue))
        }

        operations.add(LimitOperation((paginationRequest.limit + 1).toLong()))

        val results = mongoTemplate.aggregate(
            Aggregation.newAggregation(entityClass.java, operations),
            entityClass.java
        ).mappedResults

        return if (results.size > paginationRequest.limit) {
            // Has more pages
            val currentSkip = paginationRequest.pageToken.cursors["searchAfter"]?.value as? Number ?: 0
            PaginationSlice(
                items = results.take(paginationRequest.limit),
                nextPageToken = paginationRequest.pageToken.copy(
                    cursors = mapOf(
                        "searchAfter" to CursorValue(
                            value = (currentSkip.toLong() + paginationRequest.limit),
                            type = CursorValueType.NUMBER
                        )
                    )
                )
            )
        } else {
            // Last page - no next token
            PaginationSlice(
                items = results,
                nextPageToken = null
            )
        }
    }

    private fun buildShouldClauses(
        searchText: String,
        searchFields: List<String>,
        scoreFields: List<ScoreField>
    ): List<Document> {
        val clauses = mutableListOf<Document>()

        // Add regular search fields without boost
        val regularFields = searchFields.filter { field ->
            scoreFields.none { it.field == field }
        }
        if (regularFields.isNotEmpty()) {
            clauses.add(Document("text", Document().apply {
                put("query", searchText)
                put("path", regularFields)
            }))
        }

        // Add score fields with boost
        scoreFields.forEach { scoreField ->
            clauses.add(Document("text", Document().apply {
                put("query", searchText)
                put("path", scoreField.field)
                put("score", Document("boost", Document().apply {
                    when {
                        scoreField.boostPath != null -> {
                            put("path", scoreField.boostPath)
                        }
                        scoreField.boost != null -> {
                            put("value", scoreField.boost)
                        }
                        else -> {
                            put("value", 1.0)
                        }
                    }
                }))
            }))
        }

        return clauses
    }
}
