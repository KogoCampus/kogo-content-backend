package com.kogo.content.service.search

import com.kogo.content.service.pagination.PaginationRequest
import com.kogo.content.service.pagination.PaginationSlice
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import org.bson.Document
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.aggregation.AggregationOperation
import org.springframework.data.mongodb.core.aggregation.TypedAggregation

@Configuration
class MongoAtlasDao {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Value("\${search-engine.enabled:false}")
    private var searchEngineEnabled: Boolean = false

    fun <T>mockSearchDao() = object : SearchQueryDao<T> {
        override fun searchByKeyword(keyword: String, paginationRequest: PaginationRequest): PaginationSlice<T> {
            return PaginationSlice(items = emptyList(), nextPage = null)
        }
    }

    @Bean
    fun postSearchDao() = wireSearchDao(
        object : SearchQueryDao<Post> {
            @Value("\${search-engine.index.post}")
            private lateinit var atlasSearchIndex: String

            override fun searchByKeyword(keyword: String, paginationRequest: PaginationRequest): PaginationSlice<Post> {
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
                operations += buildPaginationOperations(paginationRequest)

                // Create and execute the aggregation
                val aggregation = TypedAggregation(
                    Post::class.java,
                    operations
                )

                val results = mongoTemplate.aggregate(aggregation, Post::class.java).mappedResults

                // Create next page token if we have results and we got a full page
                val limit = paginationRequest.limit
                val nextPageToken = if (results.size == limit) {
                    results.lastOrNull()?.let { paginationRequest.pageToken.nextPageToken(it.id!!) }
                } else null

                return PaginationSlice(results, nextPageToken)
            }
        }
    )

    @Bean
    fun topicSearchDao() = wireSearchDao(
        object : SearchQueryDao<Topic> {
            @Value("\${search-engine.index.topic}")
            private lateinit var atlasSearchIndex: String

            override fun searchByKeyword(keyword: String, paginationRequest: PaginationRequest): PaginationSlice<Topic> {
                val searchOperation = AggregationOperation { context ->
                    Document("\$search", Document()
                        .append("index", atlasSearchIndex)
                        .append("text", Document()
                            .append("query", keyword)
                            .append("path", listOf("topicName", "description", "tags"))
                            .append("fuzzy", Document()
                                .append("maxEdits", 2)
                                .append("prefixLength", 3)
                            )
                        )
                    )
                }

                val operations = mutableListOf(searchOperation)
                operations += buildPaginationOperations(paginationRequest)

                // Create and execute the aggregation
                val aggregation = TypedAggregation(
                    Topic::class.java,
                    operations
                )

                val results = mongoTemplate.aggregate(aggregation, Topic::class.java).mappedResults

                // Create next page token if we have results and we got a full page
                val limit = paginationRequest.limit
                val nextPageToken = if (results.size == limit) {
                    results.lastOrNull()?.let { paginationRequest.pageToken.nextPageToken(it.id!!) }
                } else null

                return PaginationSlice(results, nextPageToken)
            }
        }
    )

    private fun <T>wireSearchDao(searchQueryDao: SearchQueryDao<T>) =
        if (searchEngineEnabled) searchQueryDao
        else mockSearchDao()

    private fun buildPaginationOperations(paginationRequest: PaginationRequest): List<AggregationOperation> {
        val limit = paginationRequest.limit
        val pageLastResourceId = paginationRequest.pageToken.pageLastResourceId

        val operations = mutableListOf<AggregationOperation>()

        if (pageLastResourceId != null) {
            operations.add(AggregationOperation { context ->
                Document("\$match", Document("_id", Document("\$lt", pageLastResourceId)))
            })
        }

        // Add limit
        operations.add(AggregationOperation {
            Document("\$limit", limit)
        })

        return operations.toList()
    }
}
