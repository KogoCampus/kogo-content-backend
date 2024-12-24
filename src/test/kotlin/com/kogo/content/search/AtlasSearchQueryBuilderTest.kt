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
import java.util.Date

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
                    title = "Advanced Kotlin Coroutines",
                    content = "Deep dive into Kotlin coroutines for async programming.",
                    tags = listOf("kotlin", "coroutines", "advanced"),
                    score = 0.9,
                    viewCount = 2000,
                    createdAt = now.minusSeconds(7200),
                    popularityScore = 90.0,
                    location = Document().apply {
                        put("type", "Point")
                        put("coordinates", listOf(-74.006, 40.7128))
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
    fun `should perform wildcard search when SearchConfiguration is null`() {
        val results = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndex = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = PaginationRequest(limit = 10),
            configuration = null
        )

        assertThat(results.items).hasSize(2)
        assertThat(results.items.map { it.title }).allMatch { it.contains("Kotlin", ignoreCase = true) }
    }

    @Test
    fun `should perform compound search with text search fields and score boosting`() {
        val config = SearchConfiguration(
            textSearchFields = listOf("title", "content"),
            scoreFields = listOf(
                ScoreField(
                    field = "title",
                    score = Score.Boost(2.0)
                ),
                ScoreField(
                    field = "content",
                    score = Score.Constant(1.0)
                )
            ),
            fuzzyMaxEdits = 1
        )

        val results = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndex = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = PaginationRequest(limit = 10),
            configuration = config
        )

        assertThat(results.items).hasSize(2)
        // Title boost should make "Introduction to Kotlin" appear first
        assertThat(results.items.first().title).isEqualTo("Introduction to Kotlin")
    }

    @Test
    fun `should perform compound search with near queries`() {
        val now = Date()
        val config = SearchConfiguration(
            textSearchFields = listOf("title", "content"),
            nearFields = listOf(
                DateNearField(
                    field = "createdAt",
                    origin = now,
                    pivot = DateNearField.ONE_DAY_MS,
                    score = Score.Boost(1.5)
                ),
                NumericNearField(
                    field = "score",
                    origin = 1.0,
                    pivot = 500,
                    score = Score.Boost(1.2)
                ),
                GeoNearField(
                    field = "location",
                    origin = GeoPoint(-74.006, 40.7128), // NYC coordinates
                    pivot = 1000,
                    score = Score.Boost(1.1)
                )
            )
        )

        val results = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndex = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = PaginationRequest(limit = 10),
            configuration = config
        )

        assertThat(results.items).hasSize(2)
        // Document with location closer to NYC should appear first
        assertThat(results.items.first().id).isEqualTo("2")
    }

    @Test
    fun `should apply filters with compound search`() {
        val config = SearchConfiguration(
            textSearchFields = listOf("title", "content")
        )

        val request = PaginationRequest(limit = 10)
            .withFilter("viewCount", 1500, FilterOperator.GREATER_THAN)
            .withFilter("tags", listOf("advanced", "coroutines"), FilterOperator.IN)

        val results = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndex = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = request,
            configuration = config
        )

        assertThat(results.items).hasSize(1)
        assertThat(results.items.first().id).isEqualTo("2")
        assertThat(results.items.first().viewCount).isGreaterThan(1500)
        assertThat(results.items.first().tags).containsAnyOf("advanced", "coroutines")
    }

    @Test
    fun `should handle pagination with search after token`() {
        val config = SearchConfiguration(
            textSearchFields = listOf("title", "content")
        )

        val firstPage = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndex = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = PaginationRequest(limit = 1),
            configuration = config
        )

        assertThat(firstPage.items).hasSize(1)
        assertThat(firstPage.nextPageToken).isNotNull

        val secondPage = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndex = INDEX_NAME,
            searchText = "kotlin",
            paginationRequest = PaginationRequest(
                limit = 1,
                pageToken = firstPage.nextPageToken!!
            ),
            configuration = config
        )

        assertThat(secondPage.items).hasSize(1)
        assertThat(secondPage.items.first().id).isNotEqualTo(firstPage.items.first().id)
    }
}
