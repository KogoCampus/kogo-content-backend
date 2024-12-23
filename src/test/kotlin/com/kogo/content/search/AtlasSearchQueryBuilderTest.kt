package com.kogo.content.search

import com.kogo.content.endpoint.common.FilterOperator
import com.kogo.content.endpoint.common.PaginationRequest
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
class AtlasSearchQueryBuilderTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate,
    private val atlasSearchQueryBuilder: AtlasSearchQueryBuilder
) {

    @org.springframework.data.mongodb.core.mapping.Document("search_test_entities")
    data class TestSearchEntity(
        @Id
        val id: String,
        val title: String,
        val content: String,
        val tags: List<String>,
        val score: Double,
        val viewCount: Int,
        val createdAt: Instant,
        val popularityScore: Double,
        val location: Document? = null
    )

    companion object {
        private const val COLLECTION_NAME = "search_test_entities"
        private const val INDEX_NAME = "test_search_index"

        @JvmStatic
        @BeforeAll
        fun beforeAll(@Autowired mongoTemplate: MongoTemplate) {
            createTestData(mongoTemplate)
            createSearchIndex(mongoTemplate)
            Thread.sleep(1000)
        }

        @JvmStatic
        @AfterAll
        fun afterAll(@Autowired mongoTemplate: MongoTemplate) {
            mongoTemplate.dropCollection(COLLECTION_NAME)
            dropSearchIndex(mongoTemplate)
        }

        private fun createTestData(mongoTemplate: MongoTemplate) {
            val now = Instant.now()
            val testData = listOf(
                TestSearchEntity(
                    id = "1",
                    title = "Introduction to Kotlin",
                    content = "Learn the basics of Kotlin programming language.",
                    tags = listOf("kotlin", "programming", "beginner"),
                    score = 0.8,
                    viewCount = 1000,
                    createdAt = now.minusSeconds(3600),
                    popularityScore = 80.0,
                    location = Document().apply {
                        put("type", "Point")
                        put("coordinates", listOf(-73.935242, 40.730610))
                    }
                ),
                TestSearchEntity(
                    id = "2",
                    title = "Advanced Kotlin Coroutines programming",
                    content = "Deep dive into Kotlin coroutines for async.",
                    tags = listOf("kotlin", "coroutines", "advanced"),
                    score = 0.9,
                    viewCount = 2000,
                    createdAt = now.minusSeconds(7200),
                    popularityScore = 90.0,
                    location = Document().apply {
                        put("type", "Point")
                        put("coordinates", listOf(-73.935242, 40.730610))
                    }
                ),
                TestSearchEntity(
                    id = "3",
                    title = "Spring Boot with Kotlin",
                    content = "Building web applications using Spring Boot and Kotlin.",
                    tags = listOf("kotlin", "spring", "web"),
                    score = 0.95,
                    viewCount = 3000,
                    createdAt = now.minusSeconds(1800),
                    popularityScore = 95.0,
                    location = Document().apply {
                        put("type", "Point")
                        put("coordinates", listOf(-73.935242, 40.730610))
                    }
                ),
                TestSearchEntity(
                    id = "4",
                    title = "Java vs Kotlin Comparison",
                    content = "Comparing Java and Kotlin features and syntax.",
                    tags = listOf("kotlin", "java", "comparison"),
                    score = 0.85,
                    viewCount = 1500,
                    createdAt = now.minusSeconds(5400),
                    popularityScore = 85.0,
                    location = Document().apply {
                        put("type", "Point")
                        put("coordinates", listOf(-73.935242, 40.730610))
                    }
                ),
                TestSearchEntity(
                    id = "5",
                    title = "Kotlin Android Development",
                    content = "Mobile app development with Kotlin for Android.",
                    tags = listOf("kotlin", "android", "mobile"),
                    score = 0.75,
                    viewCount = 2500,
                    createdAt = now.minusSeconds(900),
                    popularityScore = 75.0,
                    location = Document().apply {
                        put("type", "Point")
                        put("coordinates", listOf(-73.935242, 40.730610))
                    }
                )
            )
            mongoTemplate.insertAll(testData)
        }

        private fun createSearchIndex(mongoTemplate: MongoTemplate) {
            val indexDefinition = Document().apply {
                put("mappings", Document().apply {
                    put("dynamic", false)
                    put("fields", Document().apply {
                        put("title", Document("type", "string"))
                        put("content", Document("type", "string"))
                        put("tags", Document("type", "string"))
                        put("score", Document("type", "number"))
                        put("viewCount", Document("type", "number"))
                        put("createdAt", Document("type", "date"))
                        put("popularityScore", Document("type", "number"))
                        put("location", Document("type", "geo"))
                    })
                })
            }

            val command = Document().apply {
                put("createSearchIndexes", COLLECTION_NAME)
                put("indexes", listOf(Document().apply {
                    put("name", INDEX_NAME)
                    put("definition", indexDefinition)
                }))
            }

            mongoTemplate.db.runCommand(command)
        }

        private fun dropSearchIndex(mongoTemplate: MongoTemplate) {
            val command = Document().apply {
                put("dropSearchIndex", COLLECTION_NAME)
                put("name", INDEX_NAME)
            }

            try {
                mongoTemplate.db.runCommand(command)
            } catch (e: Exception) {
                // Ignore if index doesn't exist
            }
        }
    }

    @Test
    fun `should paginate search results using search after token`() {
        val config = SearchConfiguration(
            textSearchFields = listOf("title", "content"),
            scoreFields = listOf(
                ScoreField(
                    field = "title",
                    score = Score.Boost(1.5)
                )
            )
        )

        val firstPage = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = PaginationRequest(limit = 2),
            configuration = config
        )

        assertThat(firstPage.nextPageToken).isNotNull
        assertThat(firstPage.items).hasSize(2)

        val secondPage = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = PaginationRequest(
                limit = 2,
                pageToken = firstPage.nextPageToken!!
            ),
            configuration = config
        )

        // Verify pagination
        assertThat(secondPage.items).hasSize(2)
        assertThat(firstPage.items.map { it.id })
            .doesNotContainAnyElementsOf(secondPage.items.map { it.id })
    }

    @Test
    fun `should filter search results with numeric comparison operators`() {
        val config = SearchConfiguration(
            textSearchFields = listOf("title", "content")
        )

        val request = PaginationRequest(limit = 10)
            .withFilter("viewCount", 1000, FilterOperator.GREATER_THAN)
            .withFilter("viewCount", 3000, FilterOperator.LESS_THAN)

        val results = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = request,
            configuration = config
        )

        assertThat(results.items).isNotEmpty
        assertThat(results.items.all { it.viewCount in 1001..2999 }).isTrue()
    }

    @Test
    fun `should filter search results with date comparison operators`() {
        val config = SearchConfiguration(
            textSearchFields = listOf("title", "content")
        )

        val now = Instant.now()
        val oneHourAgo = now.minusSeconds(3600)
        val twoHoursAgo = now.minusSeconds(7200)

        val request = PaginationRequest(limit = 10)
            .withFilter("createdAt", twoHoursAgo, FilterOperator.GREATER_THAN)
            .withFilter("createdAt", oneHourAgo, FilterOperator.LESS_THAN)

        val results = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = request,
            configuration = config
        )

        assertThat(results.items).isNotEmpty
        assertThat(results.items.all {
            it.createdAt.isAfter(twoHoursAgo) && it.createdAt.isBefore(oneHourAgo)
        }).isTrue()
    }
}
