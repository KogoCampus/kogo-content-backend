package com.kogo.content.search.index

import com.kogo.content.lib.*
import com.kogo.content.storage.entity.Post
import com.kogo.content.storage.entity.Topic
import com.kogo.content.storage.entity.User
import com.kogo.content.storage.view.PostAggregate
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.*

@SpringBootTest
@ActiveProfiles("test")
class PostSearchIndexTest @Autowired constructor(
    private val postSearchIndex: PostSearchIndex,
) {
    companion object {
        private lateinit var staticMongoTemplate: MongoTemplate
        private const val COLLECTION_NAME = "post_stats"
        private const val INDEX_NAME = "post_stats_search"

        @JvmStatic
        @BeforeAll
        fun beforeAll(@Autowired mongoTemplate: MongoTemplate) {
            staticMongoTemplate = mongoTemplate
            createTestData()
            createSearchIndex()
        }

        @JvmStatic
        @AfterAll
        fun afterAll() {
            cleanupSearchIndex()
        }

        private fun createTestData() {
            // Create test user
            val user = User(
                id = "user1",
                username = "testUser",
                email = "test@example.com",
                schoolName = "Test University"
            )

            // Create test topic
            val topic = Topic(
                id = "topic1",
                topicName = "Programming",
                description = "Programming related discussions",
                owner = user,
                tags = listOf("programming", "technology")
            )

            staticMongoTemplate.save(user)
            staticMongoTemplate.save(topic)

            val testData = listOf(
                PostAggregate(
                    postId = "1",
                    post = Post(
                        id = "1",
                        title = "Introduction to Kotlin Coroutines",
                        content = "Learn about Kotlin coroutines for async programming. This comprehensive guide covers the basics of coroutines and their practical applications.",
                        topic = topic,
                        author = user,
                        createdAt = Instant.now().minusSeconds(3600)
                    ),
                    likeCount = 100,
                    viewCount = 1000,
                    commentCount = 20,
                    popularityScore = 0.85,
                    lastUpdated = Instant.now()
                ),
                PostAggregate(
                    postId = "2",
                    post = Post(
                        id = "2",
                        title = "Advanced Kotlin Features",
                        content = "Deep dive into Kotlin's advanced features including coroutines, flow, and channels. Perfect for experienced developers.",
                        topic = topic,
                        author = user,
                        createdAt = Instant.now().minusSeconds(7200)
                    ),
                    likeCount = 50,
                    viewCount = 500,
                    commentCount = 10,
                    popularityScore = 0.65,
                    lastUpdated = Instant.now()
                ),
                PostAggregate(
                    postId = "3",
                    post = Post(
                        id = "3",
                        title = "Spring Boot with Kotlin",
                        content = "Building web applications using Spring Boot and Kotlin. Learn how to create robust backend services with modern tools.",
                        topic = topic,
                        author = user,
                        createdAt = Instant.now().minusSeconds(1800)
                    ),
                    likeCount = 200,
                    viewCount = 2000,
                    commentCount = 40,
                    popularityScore = 0.95,
                    lastUpdated = Instant.now()
                ),
                PostAggregate(
                    postId = "4",
                    post = Post(
                        id = "4",
                        title = "Python Django Framework",
                        content = "Learn about Python web development with Django framework. Most popular web framework for rapid development.",
                        topic = topic,
                        author = user,
                        createdAt = Instant.now().minusSeconds(900)
                    ),
                    likeCount = 500,
                    viewCount = 5000,
                    commentCount = 100,
                    popularityScore = 0.99,
                    lastUpdated = Instant.now()
                ),
                PostAggregate(
                    postId = "5",
                    post = Post(
                        id = "5",
                        title = "JavaScript Ecosystem",
                        content = "Comprehensive guide to modern JavaScript and its ecosystem including Node.js and React.",
                        topic = topic,
                        author = user,
                        createdAt = Instant.now().minusSeconds(300)
                    ),
                    likeCount = 400,
                    viewCount = 4000,
                    commentCount = 80,
                    popularityScore = 0.98,
                    lastUpdated = Instant.now()
                )
            )

            staticMongoTemplate.dropCollection(COLLECTION_NAME)
            staticMongoTemplate.insertAll(testData)
        }

        private fun createSearchIndex() {
            try {
                val database = staticMongoTemplate.db

                // Drop existing index if any
                try {
                    database.runCommand(org.bson.Document().apply {
                        put("dropSearchIndex", COLLECTION_NAME)
                        put("name", INDEX_NAME)
                    })
                    Thread.sleep(1000)
                } catch (e: Exception) {
                    // Ignore if index doesn't exist
                }

                // Create new index with dynamic mapping
                val indexDefinition = """
                {
                    "mappings": {
                        "dynamic": true
                    }
                }
                """.trimIndent()

                database.runCommand(org.bson.Document().apply {
                    put("createSearchIndexes", COLLECTION_NAME)
                    put("indexes", listOf(org.bson.Document().apply {
                        put("name", INDEX_NAME)
                        put("definition", org.bson.Document.parse(indexDefinition))
                    }))
                })

                // Wait for index to be ready
                var indexReady = false
                var attempts = 0
                while (!indexReady && attempts < 30) {
                    Thread.sleep(1000)
                    attempts++

                    try {
                        val listIndexesCommand = org.bson.Document("listSearchIndexes", COLLECTION_NAME)
                        val result = database.runCommand(listIndexesCommand)

                        val cursor = result.get("cursor") as? org.bson.Document
                        val firstBatch = cursor?.get("firstBatch") as? List<*>

                        val indexStatus = firstBatch
                            ?.firstOrNull {
                                (it as? org.bson.Document)?.get("name") == INDEX_NAME
                            } as? org.bson.Document

                        when (indexStatus?.get("status")) {
                            "READY" -> {
                                println("Index is READY")
                                indexReady = true
                            }
                            "PENDING" -> {
                                println("Index is PENDING, waiting... (attempt ${attempts + 1})")
                            }
                            null -> {
                                println("No status found, waiting... (attempt ${attempts + 1})")
                            }
                            else -> {
                                val status = indexStatus.get("status")
                                println("Unexpected index status: $status (attempt ${attempts + 1})")
                            }
                        }
                    } catch (e: Exception) {
                        println("Warning: Error checking index status: ${e.message}")
                    }
                }

                if (!indexReady) {
                    throw IllegalStateException("Search index not ready after 30 seconds")
                }
            } catch (e: Exception) {
                throw IllegalStateException("Failed to create search index: ${e.message}", e)
            }
        }

        private fun cleanupSearchIndex() {
            try {
                staticMongoTemplate.db.runCommand(org.bson.Document().apply {
                    put("dropSearchIndex", COLLECTION_NAME)
                    put("name", INDEX_NAME)
                })
                staticMongoTemplate.dropCollection(COLLECTION_NAME)
            } catch (e: Exception) {
                println("Failed to cleanup: ${e.message}")
            }
        }
    }

    @Test
    fun `should find posts by title and content`() {
        val result = postSearchIndex.search(
            searchText = "kotlin",
            paginationRequest = PaginationRequest(limit = 10)
        )

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.post.title }).allSatisfy {
            assertThat(it).containsIgnoringCase("kotlin")
        }
    }

    @Test
    fun `should handle specific search terms`() {
        val result = postSearchIndex.search(
            searchText = "coroutines",
            paginationRequest = PaginationRequest(limit = 10)
        )

        assertThat(result.items).hasSize(2)
        assertThat(result.items.map { it.post.title + it.post.content })
            .allSatisfy {
                assertThat(it).containsIgnoringCase("coroutine")
            }
    }

    @Test
    fun `should handle empty results`() {
        val result = postSearchIndex.search(
            searchText = "nonexistent",
            paginationRequest = PaginationRequest(limit = 10)
        )

        assertThat(result.items).isEmpty()
        assertThat(result.nextPageToken).isNull()
    }

    @Test
    fun `should find posts within specific topic`() {
        val result = postSearchIndex.search(
            searchText = "kotlin programming",
            paginationRequest = PaginationRequest(limit = 10)
        )

        assertThat(result.items).hasSize(3)
        assertThat(result.items).allSatisfy { postAggregate ->
            assertThat(postAggregate.post.topic.topicName).isEqualTo("Programming")
            assertThat(postAggregate.post.author.username).isEqualTo("testUser")
        }
    }

    @Test
    fun `should return posts ordered by popularity and relevance`() {
        val result = postSearchIndex.search(
            searchText = "kotlin",
            paginationRequest = PaginationRequest(limit = 10),
        )

        assertThat(result.items).hasSize(3)

        // Spring Boot post should be first due to highest popularity score
        val firstPost = result.items[0]
        assertThat(firstPost.post.title).contains("Spring Boot")
        assertThat(firstPost.popularityScore).isEqualTo(0.95)
        assertThat(firstPost.post.author.username).isEqualTo("testUser")

        // Verify descending order by popularity score
        assertThat(result.items.map { it.popularityScore })
            .isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    fun `should handle field aliases in sort fields`() {
        val paginationRequest = PaginationRequest(
            limit = 10,
            pageToken = PageToken(
                sortFields = listOf(
                    SortField("createdAt", SortDirection.DESC),  // Using alias instead of post.createdAt
                    SortField("popularityScore", SortDirection.DESC)  // Non-aliased field
                )
            )
        )

        val result = postSearchIndex.search(
            searchText = "kotlin",
            paginationRequest = paginationRequest
        )

        assertThat(result.items).hasSize(3)

        // Verify results are sorted by createdAt in descending order
        assertThat(result.items.map { it.post.createdAt })
            .isSortedAccordingTo(Comparator.reverseOrder())
    }

    @Test
    fun `should handle field aliases in filters`() {
        val paginationRequest = PaginationRequest(
            limit = 10,
            pageToken = PageToken(
                filters = listOf(
                    FilterField("title", "Spring Boot", FilterOperator.EQUALS)  // Using alias instead of post.title
                )
            )
        )

        val result = postSearchIndex.search(
            searchText = "kotlin",
            paginationRequest = paginationRequest
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].post.title).contains("Spring Boot")
    }
}
