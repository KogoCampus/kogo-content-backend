package com.kogo.content.service.search

import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationResponse
import com.kogo.content.storage.entity.Post
import org.bson.Document
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.TypedAggregation
import org.springframework.stereotype.Component

@Component
internal class PostSearchService(private val mongoTemplate: MongoTemplate) : SearchService<Post> {

    @Value("\${search-engine.index.post}")
    private lateinit var atlasSearchIndex: String

    override fun searchByKeyword(keyword: String, paginationRequest: PaginationRequest): PaginationResponse<Post> {
        val limit = paginationRequest.limit
        val pageLastResourceId = paginationRequest.pageToken.pageLastResourceId

        // Create search operation
        val searchOperation = AggregationOperation { context ->
            Document("\$search", Document()
                .append("index", atlasSearchIndex)
                .append("text", Document()
                    .append("query", keyword)
                    .append("path", listOf("title", "content"))
                    .append("fuzzy", Document()
                        .append("maxEdits", 2)
                        .append("prefixLength", 3)
                    )
                )
            )
        }

        val operations = mutableListOf(searchOperation)

        // Add pagination if there's a last resource ID
        if (pageLastResourceId != null) {
            operations.add(AggregationOperation { context ->
                Document("\$match", Document("_id", Document("\$lt", pageLastResourceId)))
            })
        }

        // Add limit
        operations.add(AggregationOperation {
            Document("\$limit", limit)
        })

        // Create and execute the aggregation
        val aggregation = TypedAggregation(
            Post::class.java,
            operations
        )

        val results = mongoTemplate.aggregate(aggregation, Post::class.java).mappedResults

        // Create next page token if we have results and we got a full page
        val nextPageToken = if (results.size == limit) {
            results.lastOrNull()?.let { paginationRequest.pageToken.nextPageToken(it.id!!) }
        } else null

        return PaginationResponse(results, nextPageToken)
    }
}
