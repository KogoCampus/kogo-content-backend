package com.kogo.content.search

import com.kogo.content.common.*
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.stereotype.Component
import kotlin.reflect.KClass

@Component
class AtlasSearchQueryBuilder(
    private val mongoTemplate: MongoTemplate
) {
    companion object {
        private const val ATLAS_SEARCH_MAX_RESULTS = 10
    }

    fun <T : Any> search(
        entityClass: KClass<T>,
        searchIndexName: String,
        paginationRequest: PaginationRequest,
        searchText: String,
        searchableFields: List<String>,
        scoreFields: List<ScoreField>? = null,
    ): PaginationSlice<T> {
        val operations = mutableListOf<AggregationOperation>()

        operations.addAll(buildSearchOperations(
            searchIndexName = searchIndexName,
            searchText = searchText,
            searchableFields = searchableFields,
            scoreFields = scoreFields,
            paginationRequest = paginationRequest,
        ))

        val results = mongoTemplate.aggregate(
            Aggregation.newAggregation(entityClass.java, operations),
            entityClass.java
        )

        return createPaginationSlice(results, paginationRequest)
    }

    private fun buildSearchOperations(
        searchIndexName: String,
        searchText: String,
        searchableFields: List<String>,
        scoreFields: List<ScoreField>?,
        paginationRequest: PaginationRequest,
    ): List<AggregationOperation> = listOf(
        // Search operation
        AggregationOperation {
            Document("\$search", Document().apply {
                put("index", searchIndexName)
                put("compound", buildCompoundQuery(searchText, searchableFields, scoreFields, paginationRequest.pageToken.filters))
                put("sort", buildSortCriteria(paginationRequest.pageToken.sortFields))
                // Add searchAfter if cursor exists
                paginationRequest.pageToken.cursors["searchAfter"]?.let { cursor ->
                    put("searchAfter", cursor.value)
                }
            })
        },
        // Add metadata fields
        AggregationOperation {
            Document("\$addFields", Document().apply {
                put("_searchScore", Document("\$meta", "searchScore"))
                put("_searchAfter", Document("\$meta", "searchSequenceToken"))
            })
        },
        // Limit results
        AggregationOperation {
            Document("\$limit", minOf(paginationRequest.limit, ATLAS_SEARCH_MAX_RESULTS))
        }
    )

    private fun buildCompoundQuery(
        searchText: String,
        searchFields: List<String>,
        scoreFields: List<ScoreField>?,
        filters: List<FilterField>
    ) = Document().apply {
        // Must clause for text search
        put("must", listOf(Document("text", Document().apply {
            put("query", searchText)
            put("path", searchFields)
            put("fuzzy", Document("maxEdits", 1))
        })))

        // Should clause for boosting
        if (!scoreFields.isNullOrEmpty()) {
            put("should", scoreFields.map { scoreField ->
                Document("text", Document().apply {
                    put("query", searchText)
                    put("path", scoreField.field)
                    when {
                        scoreField.boostPath != null -> {
                            put("score", Document("boost", Document("path", scoreField.boostPath)))
                        }
                        scoreField.boost != null -> {
                            put("score", Document("boost", Document("value", scoreField.boost)))
                        }
                    }
                })
            })
        }

        // Filter clause
        if (filters.isNotEmpty()) {
            put("filter", buildFilterCriteria(filters))
        }
    }

    private fun buildFilterCriteria(filters: List<FilterField>) = filters.map { filter ->
        when (filter.operator) {
            FilterOperator.EQUALS -> when (filter.value) {
                is Number -> Document("equals", Document().apply {
                    put("path", filter.field)
                    put("value", filter.value)
                })
                else -> Document("text", Document().apply {
                    put("query", filter.value.toString())
                    put("path", filter.field)
                })
            }
            FilterOperator.IN -> Document("queryString", Document().apply {
                put("defaultPath", filter.field)
                put("query", when (filter.value) {
                    is List<*> -> filter.value.joinToString(" OR ")
                    is Array<*> -> filter.value.joinToString(" OR ")
                    else -> filter.value.toString()
                })
            })
        }
    }

    private fun buildSortCriteria(sortFields: List<SortField>) = Document().apply {
        put("score", Document("\$meta", "searchScore"))
        sortFields.forEach { sortField ->
            put(sortField.field, if (sortField.direction == SortDirection.ASC) 1 else -1)
        }
    }

    private fun <T> createPaginationSlice(results: AggregationResults<T>, paginationRequest: PaginationRequest): PaginationSlice<T> {
        val rawResults = results.rawResults["results"] as? List<Document>
        val mappedResults = results.mappedResults

        if (mappedResults.isEmpty()) {
            return PaginationSlice(items = emptyList(), nextPageToken = null)
        }

        val limit = minOf(paginationRequest.limit, ATLAS_SEARCH_MAX_RESULTS)
        // Create next page token only if we got a full page of results
        val nextPageToken = if (mappedResults.size >= limit) {
            // Get the last document's search sequence token
            val lastDocument = rawResults?.last()
            val searchAfter = lastDocument?.getString("_searchAfter")!!

            paginationRequest.pageToken.copy(
                cursors = mapOf(
                    "searchAfter" to CursorValue.from(searchAfter)
                ),
                sortFields = paginationRequest.pageToken.sortFields,
                filters = paginationRequest.pageToken.filters
            )
        } else null

        return PaginationSlice(
            items = mappedResults,
            nextPageToken = nextPageToken
        )
    }
}
