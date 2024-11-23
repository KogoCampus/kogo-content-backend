package com.kogo.content.search

import com.kogo.content.common.*
import org.bson.Document
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.Aggregation
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.AggregationResults
import org.springframework.stereotype.Component
import java.util.Date
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
        configuration: SearchConfiguration,
    ): PaginationSlice<T> {
        val operations = mutableListOf<AggregationOperation>()

        operations.addAll(buildSearchOperations(
            searchIndexName = searchIndexName,
            searchText = searchText,
            configuration = configuration,
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
        configuration: SearchConfiguration,
        paginationRequest: PaginationRequest,
    ): List<AggregationOperation> = listOf(
        // Search operation
        AggregationOperation {
            Document("\$search", Document().apply {
                put("index", searchIndexName)
                put("compound", buildCompoundQuery(searchText, configuration, paginationRequest.pageToken.filters))
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

    private fun buildScoreDocument(score: Score): Document = when (score) {
        is Score.Boost -> Document("boost", Document("value", score.value))
        is Score.Constant -> Document("constant", Document("value", score.value))
        is Score.Path -> Document("boost", Document("path", score.path))
        is Score.Function -> Document("function", score.expression)
    }

    private fun buildCompoundQuery(
        searchText: String,
        configuration: SearchConfiguration,
        filters: List<FilterField>
    ) = Document().apply {
        // Must clause for text search
        put("must", listOf(Document("text", Document().apply {
            put("query", searchText)
            put("path", configuration.textSearchFields)
            put("fuzzy", Document("maxEdits", configuration.fuzzyMaxEdits))
        })))

        // Should clause for boosting and near queries
        val shouldClauses = mutableListOf<Document>()

        // Add score field boosts
        configuration.scoreFields.forEach { scoreField ->
            shouldClauses.add(Document("text", Document().apply {
                put("query", searchText)
                put("path", scoreField.field)
                put("score", buildScoreDocument(scoreField.score))
            }))
        }

        // Add near field boosts
        configuration.nearFields.forEach { nearField ->
            shouldClauses.add(Document("near", Document().apply {
                put("path", nearField.field)
                put("origin", when (nearField) {
                    is DateNearField -> nearField.origin
                    is NumericNearField -> nearField.origin
                    is GeoNearField -> nearField.origin.toDocument()
                })
                put("pivot", nearField.pivot)
                nearField.score?.let { score ->
                    put("score", buildScoreDocument(score))
                }
            }))
        }

        if (shouldClauses.isNotEmpty()) {
            put("should", shouldClauses)
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

    private fun <T> createPaginationSlice(results: AggregationResults<T>, paginationRequest: PaginationRequest): PaginationSlice<T> {
        val rawResults = results.rawResults["results"] as? List<Document>
        val mappedResults = results.mappedResults

        if (mappedResults.isEmpty()) {
            return PaginationSlice(items = emptyList(), nextPageToken = null)
        }

        val limit = minOf(paginationRequest.limit, ATLAS_SEARCH_MAX_RESULTS)
        // Create next page token only if we got a full page of results
        val nextPageToken = if (mappedResults.size >= limit) {
            val lastSearchDocument = rawResults?.last()
            val searchAfter = lastSearchDocument?.getString("_searchAfter")

            searchAfter?.let {
                paginationRequest.pageToken.copy(
                    cursors = mapOf("searchAfter" to CursorValue.from(it))
                )
            }
        } else null

        return PaginationSlice(
            items = mappedResults,
            nextPageToken = nextPageToken
        )
    }
}
