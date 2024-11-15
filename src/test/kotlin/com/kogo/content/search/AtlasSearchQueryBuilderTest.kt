package com.kogo.content.search

import com.kogo.content.lib.PageToken
import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.SortDirection
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
class AtlasSearchQueryBuilderTest @Autowired constructor(
    private val atlasSearchQueryBuilder: AtlasSearchQueryBuilder
) {
    companion object {
        private lateinit var staticMongoTemplate: MongoTemplate
        private const val searchIndex = "test_search"
        private val searchFields = listOf("title", "content", "summary", "tags")

        @JvmStatic
        @BeforeAll
        fun beforeAll(@Autowired mongoTemplate: MongoTemplate) {
            staticMongoTemplate = mongoTemplate
            createSearchIndex()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            cleanupSearchIndex()
        }

        private fun createSearchIndex() {
            try {
                staticMongoTemplate.dropCollection(TestSearchEntity::class.java)
                val testData = listOf(
                    TestSearchEntity(
                        id = "1",
                        title = "Getting Started with MongoDB Atlas Search",
                        content = "Comprehensive guide to implementing full-text search in MongoDB Atlas. Learn about analyzers, scoring, and boosting.",
                        summary = "Learn MongoDB Atlas Search basics",
                        tags = listOf("mongodb", "atlas", "search", "beginner"),
                        createdAt = Instant.now().minusSeconds(3600),
                        viewCount = 1500,
                        likeCount = 120,
                        commentCount = 25,
                        score = 0.85,
                        authorReputation = 0.92,
                        isVerified = true,
                        category = "tutorial",
                        language = "en"
                    ),
                    TestSearchEntity(
                        id = "2",
                        title = "Advanced MongoDB Search Techniques",
                        content = "Deep dive into MongoDB Atlas Search advanced features including compound queries, facets, and custom scoring.",
                        summary = "Advanced search features in MongoDB",
                        tags = listOf("mongodb", "atlas", "search", "advanced"),
                        createdAt = Instant.now().minusSeconds(7200),
                        viewCount = 800,
                        likeCount = 95,
                        commentCount = 15,
                        score = 0.78,
                        authorReputation = 0.85,
                        isVerified = true,
                        category = "advanced",
                        language = "en"
                    ),
                    TestSearchEntity(
                        id = "3",
                        title = "MongoDB Performance Optimization Guide",
                        content = "Learn how to optimize MongoDB queries and indexes for better performance. Includes Atlas Search optimization tips.",
                        summary = "Optimize MongoDB and Atlas Search",
                        tags = listOf("mongodb", "performance", "optimization"),
                        createdAt = Instant.now().minusSeconds(1800),
                        viewCount = 2500,
                        likeCount = 230,
                        commentCount = 45,
                        score = 0.95,
                        authorReputation = 0.88,
                        isVerified = true,
                        category = "performance",
                        language = "en"
                    ),
                    TestSearchEntity(
                        id = "4",
                        title = "Building a Search Engine with Atlas Search",
                        content = "Step-by-step tutorial on building a search engine using MongoDB Atlas Search. Covers relevance tuning and highlighting.",
                        summary = "Create search engine with Atlas Search",
                        tags = listOf("mongodb", "atlas", "search", "tutorial"),
                        createdAt = Instant.now().minusSeconds(900),
                        viewCount = 1200,
                        likeCount = 150,
                        commentCount = 30,
                        score = 0.82,
                        authorReputation = 0.95,
                        isVerified = true,
                        category = "tutorial",
                        language = "en"
                    ),
                    TestSearchEntity(
                        id = "5",
                        title = "MongoDB Text Search vs Atlas Search",
                        content = "Comparison between MongoDB's text search and Atlas Search capabilities. Analysis of features and performance.",
                        summary = "Compare search options in MongoDB",
                        tags = listOf("mongodb", "comparison", "search"),
                        createdAt = Instant.now().minusSeconds(300),
                        viewCount = 3000,
                        likeCount = 280,
                        commentCount = 60,
                        score = 0.91,
                        authorReputation = 0.89,
                        isVerified = true,
                        category = "comparison",
                        language = "en"
                    )
                )
                staticMongoTemplate.insertAll(testData)
            } catch (e: Exception) {
                throw IllegalStateException("Failed to prepare the test entity collection: ${e.message}", e)
            }

            try {
                val database = staticMongoTemplate.db

                // First, try to drop any existing index
                try {
                    val dropCommand = Document().apply {
                        put("dropSearchIndex", "test_search_entities")
                        put("name", searchIndex)
                    }
                    database.runCommand(dropCommand)
                    Thread.sleep(1000) // Wait for index to be dropped
                } catch (e: Exception) {
                    // Ignore if index doesn't exist
                }

                // Create Atlas Search index
                val indexDefinition = """
                {
                  "mappings": {
                    "dynamic": false,
                    "fields": {
                      "title": {
                        "type": "string",
                        "analyzer": "lucene.standard"
                      },
                      "content": {
                        "type": "string",
                        "analyzer": "lucene.standard"
                      },
                      "summary": {
                        "type": "string",
                        "analyzer": "lucene.standard"
                      },
                      "tags": {
                        "type": "string",
                        "analyzer": "lucene.standard"
                      },
                      "viewCount": {
                        "type": "number"
                      },
                      "likeCount": {
                        "type": "number"
                      },
                      "commentCount": {
                        "type": "number"
                      },
                      "score": {
                        "type": "number"
                      },
                      "authorReputation": {
                        "type": "number"
                      },
                      "isVerified": {
                        "type": "boolean"
                      },
                      "category": {
                        "type": "string"
                      },
                      "language": {
                        "type": "string"
                      }
                    }
                  }
                }
                """.trimIndent()

                val command = Document().apply {
                    put("createSearchIndexes", "test_search_entities")
                    put("indexes", listOf(Document().apply {
                        put("name", searchIndex)
                        put("definition", Document.parse(indexDefinition))
                    }))
                }

                database.runCommand(command)

                // Wait for index to be ready
                var indexReady = false
                var attempts = 0
                while (!indexReady && attempts < 30) { // timeout to 30 seconds
                    try {
                        val listIndexesCommand = Document("listSearchIndexes", "test_search_entities")
                        val indexes = database.runCommand(listIndexesCommand)

                        val cursor = indexes.get("cursor") as? Document
                        val firstBatch = cursor?.get("firstBatch") as? List<*>

                        val indexStatus = firstBatch
                            ?.firstOrNull {
                                (it as? Document)?.get("name") == searchIndex
                            } as? Document

                        when (indexStatus?.get("status")) {
                            "READY" -> {
                                println("Index is READY")
                                indexReady = true
                            }
                            "PENDING" -> {
                                println("Index is PENDING, waiting... (attempt ${attempts + 1})")
                                Thread.sleep(1000)
                            }
                            null -> {
                                println("No status found, waiting... (attempt ${attempts + 1})")
                                Thread.sleep(1000)
                            }
                            else -> {
                                val status = indexStatus.get("status")
                                println("Unexpected index status: $status (attempt ${attempts + 1})")
                                Thread.sleep(1000)
                            }
                        }
                    } catch (e: Exception) {
                        println("Warning: Error checking index status: ${e.message}")
                        Thread.sleep(1000)
                    }

                    attempts++
                }

                if (!indexReady) {
                    throw IllegalStateException("Search index not ready after 60 seconds")
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create search index: ${e.message}", e)
            }
        }

        private fun cleanupSearchIndex() {
            try {
                staticMongoTemplate.executeCommand(org.bson.Document().apply {
                    put("dropSearchIndex", "test_search_entities")
                    put("name", searchIndex)
                })
                staticMongoTemplate.dropCollection(TestSearchEntity::class.java)
            } catch (e: Exception) {
                println("Failed to drop search index: ${e.message}")
            }
        }
    }

    @org.springframework.data.mongodb.core.mapping.Document("test_search_entities")
    data class TestSearchEntity(
        @Id
        val id: String,
        val title: String,
        val content: String,
        val summary: String,
        val tags: List<String>,
        @Field("created_at")
        val createdAt: Instant,
        val viewCount: Long,
        val likeCount: Long,
        val commentCount: Long,
        val score: Double,  // Composite engagement score
        val authorReputation: Double,  // Author's reputation score
        val isVerified: Boolean,
        val category: String,
        val language: String
    )

    @Test
    fun `should perform basic text search without score fields`() {
        val request = PaginationRequest(limit = 3)

        val result = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            request,
            "performance optimization",  // More specific search term
            searchFields,
            scoreFields = null
        )

        assertThat(result.items).hasSize(2)
        // Should find performance-related content
        assertThat(result.items.map { it.title + it.content }).allSatisfy {
            assertThat(it).containsIgnoringCase("performance")
        }
    }

    @Test
    fun `should boost specific fields with value boost`() {
        val request = PaginationRequest(limit = 3)

        val result = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            request,
            "MongoDB Search",
            searchFields,
            scoreFields = listOf(
                AtlasSearchQueryBuilder.ScoreField(
                    field = "title",
                    boost = 3.0  // Higher boost for title matches
                ),
                AtlasSearchQueryBuilder.ScoreField(
                    field = "tags",
                    boost = 2.0  // Medium boost for tag matches
                )
            )
        )

        // Title matches should be prioritized due to boost
        assertThat(result.items).hasSize(3)
        assertThat(result.items[0].title).containsIgnoringCase("MongoDB")
    }

    @Test
    fun `should boost fields using engagement metrics`() {
        val request = PaginationRequest(limit = 3)

        val result = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            request,
            "MongoDB Atlas",
            searchFields,
            scoreFields = listOf(
                AtlasSearchQueryBuilder.ScoreField(
                    field = "title",
                    boostPath = "score"  // Use composite engagement score
                ),
                AtlasSearchQueryBuilder.ScoreField(
                    field = "content",
                    boostPath = "authorReputation"  // Consider author reputation
                )
            )
        )

        // Results should be influenced by engagement and reputation
        assertThat(result.items).hasSize(3)
        assertThat(result.items[0].score).isGreaterThanOrEqualTo(0.9)
        assertThat(result.items[0].authorReputation).isGreaterThanOrEqualTo(0.85)
    }

    @Test
    fun `should combine multiple score fields with different criteria`() {
        val request = PaginationRequest(limit = 3)

        val result = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            request,
            "Atlas Search Guide",
            searchFields,
            scoreFields = listOf(
                AtlasSearchQueryBuilder.ScoreField(
                    field = "title",
                    boost = 2.5  // High boost for title matches
                ),
                AtlasSearchQueryBuilder.ScoreField(
                    field = "summary",
                    boost = 1.5  // Medium boost for summary matches
                ),
                AtlasSearchQueryBuilder.ScoreField(
                    field = "content",
                    boostPath = "score"  // Use engagement score for content relevance
                )
            )
        )

        assertThat(result.items).hasSize(3)
        // Should prioritize comprehensive guides with good engagement
        assertThat(result.items[0].title).containsIgnoringCase("Guide")
        assertThat(result.items[0].score).isGreaterThanOrEqualTo(0.8)
    }

    @Test
    fun `should handle pagination with complex scoring`() {
        val firstRequest = PaginationRequest(limit = 2)

        val scoreFields = listOf(
            AtlasSearchQueryBuilder.ScoreField(
                field = "title",
                boost = 2.0
            ),
            AtlasSearchQueryBuilder.ScoreField(
                field = "content",
                boostPath = "score"
            )
        )

        val firstResult = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            firstRequest,
            "MongoDB Atlas Search Features",
            searchFields,
            scoreFields = scoreFields
        )

        val secondRequest = PaginationRequest(
            limit = 2,
            pageToken = firstResult.nextPageToken!!
        )

        val secondResult = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            secondRequest,
            "MongoDB Atlas Search Features",
            searchFields,
            scoreFields = scoreFields
        )

        assertThat(secondResult.items).hasSize(2)
        assertThat(secondResult.items).isNotEqualTo(firstResult.items)
        // Verify all results are relevant
        assertThat(secondResult.items + firstResult.items).allSatisfy {
            assertThat(it.title + it.content).containsAnyOf("Atlas", "Search", "Features")
        }
    }

    @Test
    fun `should handle empty search results with score fields`() {
        val request = PaginationRequest(limit = 10)

        val result = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            request,
            "NonexistentTerm",
            searchFields,
            scoreFields = listOf(
                AtlasSearchQueryBuilder.ScoreField(
                    field = "title",
                    boost = 2.0
                )
            )
        )

        assertThat(result.items).isEmpty()
        assertThat(result.nextPageToken).isNull()
    }

    @Test
    fun `should handle pagination with token encoding and decoding`() {
        val initialRequest = PaginationRequest(limit = 2)
            .withSort("createdAt", SortDirection.DESC)

        // First page
        val firstResult = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            initialRequest,
            "MongoDB",
            searchFields
        )

        assertThat(firstResult.items).hasSize(2)
        assertThat(firstResult.nextPageToken).isNotNull
        assertThat(firstResult.items.map { it.title }).containsExactly(
            "MongoDB Text Search vs Atlas Search",        // newest (300s ago)
            "Building a Search Engine with Atlas Search"  // (900s ago)
        )

        // Encode and decode token
        val encodedToken = firstResult.nextPageToken?.encode()
        assertThat(encodedToken).isNotNull

        val decodedToken = PageToken.fromString(encodedToken!!)
        assertThat(decodedToken.sortFields).anySatisfy { sortField ->
            assertThat(sortField.field).isEqualTo("createdAt")
            assertThat(sortField.direction).isEqualTo(SortDirection.DESC)
        }

        // Second page using decoded token
        val secondResult = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            PaginationRequest(
                limit = 2,
                pageToken = decodedToken
            ),
            "MongoDB",
            searchFields
        )

        assertThat(secondResult.items).hasSize(2)
        assertThat(secondResult.nextPageToken).isNotNull
        assertThat(secondResult.items.map { it.title }).containsExactly(
            "MongoDB Performance Optimization Guide",     // (1800s ago)
            "Getting Started with MongoDB Atlas Search"   // (3600s ago)
        )

        // Last page using next token
        val lastResult = atlasSearchQueryBuilder.search(
            TestSearchEntity::class,
            searchIndex,
            PaginationRequest(
                limit = 2,
                pageToken = secondResult.nextPageToken!!
            ),
            "MongoDB",
            searchFields
        )
        println(lastResult.items)
        assertThat(lastResult.items).hasSize(1)
        assertThat(lastResult.nextPageToken).isNull()
        assertThat(lastResult.items.map { it.title }).containsExactly(
            "Advanced MongoDB Search Techniques"          // oldest (7200s ago)
        )

        // Verify all results are in correct order
        val allResults = firstResult.items + secondResult.items + lastResult.items
        assertThat(allResults).hasSize(5)
        assertThat(allResults.map { it.id }).doesNotHaveDuplicates()
        assertThat(allResults.map { it.createdAt }).isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    fun `should handle search with filters and sorting across multiple pages`() {
        // TODO
    }
}
