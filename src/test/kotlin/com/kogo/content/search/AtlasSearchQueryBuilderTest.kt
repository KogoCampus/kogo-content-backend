package com.kogo.content.search

import com.kogo.content.common.*
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
        val createdAt: Instant
    )

    companion object {
        private const val COLLECTION_NAME = "search_test_entities"
        private const val INDEX_NAME = "test_search_index"

        @JvmStatic
        @BeforeAll
        fun beforeAll(@Autowired mongoTemplate: MongoTemplate) {
            // Create test data
            createTestData(mongoTemplate)
            // Create search index
            createSearchIndex(mongoTemplate)
            // Wait for index to be ready
            Thread.sleep(1000)
        }

        @JvmStatic
        @AfterAll
        fun afterAll(@Autowired mongoTemplate: MongoTemplate) {
            mongoTemplate.dropCollection(COLLECTION_NAME)
            dropSearchIndex(mongoTemplate)
        }

        private fun createTestData(mongoTemplate: MongoTemplate) {
            val testData = listOf(
                TestSearchEntity(
                    id = "1",
                    title = "Introduction to Kotlin",
                    content = "Learn the basics of Kotlin programming language. Perfect for beginners.",
                    tags = listOf("kotlin", "programming", "beginner"),
                    score = 0.8,
                    viewCount = 1000,
                    createdAt = Instant.now().minusSeconds(3600)
                ),
                TestSearchEntity(
                    id = "2",
                    title = "Advanced Kotlin Coroutines programming",
                    content = "Deep dive into Kotlin coroutines for async.",
                    tags = listOf("kotlin", "coroutines", "advanced"),
                    score = 0.9,
                    viewCount = 2000,
                    createdAt = Instant.now().minusSeconds(7200)
                ),
                TestSearchEntity(
                    id = "3",
                    title = "Spring Boot with Kotlin",
                    content = "Building web applications using Spring Boot and Kotlin.",
                    tags = listOf("kotlin", "spring", "web"),
                    score = 0.95,
                    viewCount = 3000,
                    createdAt = Instant.now().minusSeconds(1800)
                ),
                TestSearchEntity(
                    id = "4",
                    title = "Java vs Kotlin Comparison",
                    content = "Comparing Java and Kotlin features and syntax.",
                    tags = listOf("kotlin", "java", "comparison"),
                    score = 0.85,
                    viewCount = 1500,
                    createdAt = Instant.now().minusSeconds(5400)
                ),
                TestSearchEntity(
                    id = "5",
                    title = "Kotlin Android Development",
                    content = "Mobile app development with Kotlin for Android.",
                    tags = listOf("kotlin", "android", "mobile"),
                    score = 0.75,
                    viewCount = 2500,
                    createdAt = Instant.now().minusSeconds(900)
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
    fun `should search text across searchable fields`() {
        val result = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin programming",
            searchableFields = listOf("title", "content"),
            paginationRequest = PaginationRequest(limit = 10)
        )

        assertThat(result.items).hasSize(5)
        assertThat(result.items.map { it.title }).allMatch { it.contains("Kotlin", ignoreCase = true) }
    }

    @Test
    fun `should boost search results based on score`() {
        val result = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "programming",
            searchableFields = listOf("title", "content"),
            scoreFields = listOf(ScoreField("content", boost = 2.0)),
            paginationRequest = PaginationRequest(limit = 3)
        )

        assertThat(result.items).hasSize(2)
        // Spring Boot should be first due to highest score
        assertThat(result.items[0].title).isEqualTo("Introduction to Kotlin")
        assertThat(result.items[0].score).isEqualTo(0.8)
    }

    @Test
    fun `should sort search results`() {
        val request = PaginationRequest(
            limit = 5,
            pageToken = PageToken(
                sortFields = listOf(
                    SortField("viewCount", SortDirection.DESC)
                )
            )
        )

        val result = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            searchableFields = listOf("title"),
            paginationRequest = request
        )

        assertThat(result.items).hasSize(5)
        assertThat(result.items.map { it.viewCount })
            .containsExactly(2500, 1000, 3000, 2000, 1500)
    }

    @Test
    fun `should apply multiple sorts`() {
        val request = PaginationRequest(
            limit = 5,
            pageToken = PageToken(
                sortFields = listOf(
                    SortField("score", SortDirection.DESC),
                    SortField("viewCount", SortDirection.DESC)
                )
            )
        )

        val result = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            searchableFields = listOf("title", "content"),
            paginationRequest = request
        )

        assertThat(result.items).hasSize(5)
        // Verify primary sort by score, secondary by viewCount
        assertThat(result.items.map { it.score to it.viewCount })
            .isSortedAccordingTo(compareByDescending<Pair<Double, Int>> { it.first }
                .thenByDescending { it.second })
    }

    @Test
    fun `should filter search results`() {
        val request = PaginationRequest(
            limit = 5,
            pageToken = PageToken(
                filters = listOf(
                    FilterField("tags", "advanced", FilterOperator.EQUALS)
                )
            )
        )

        val result = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            searchableFields = listOf("title", "content"),
            paginationRequest = request
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].title).contains("Advanced")
    }

    @Test
    fun `should apply multiple filters`() {
        val request = PaginationRequest(
            limit = 5,
            pageToken = PageToken(
                filters = listOf(
                    FilterField("score", 0.8, FilterOperator.EQUALS),
                    FilterField("viewCount", 1000, FilterOperator.EQUALS)
                )
            )
        )

        val result = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            searchableFields = listOf("title", "content"),
            paginationRequest = request
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items[0].score).isEqualTo(0.8)
        assertThat(result.items[0].viewCount).isEqualTo(1000)
    }

    @Test
    fun `should handle pagination`() {
        val firstRequest = PaginationRequest(
            limit = 2,
            pageToken = PageToken(
                sortFields = listOf(SortField("createdAt", SortDirection.DESC))
            )
        )

        val firstPage = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            searchableFields = listOf("title", "content"),
            paginationRequest = firstRequest
        )

        assertThat(firstPage.items).hasSize(2)
        assertThat(firstPage.nextPageToken).isNotNull

        // Get second page
        val secondPage = atlasSearchQueryBuilder.search(
            entityClass = TestSearchEntity::class,
            searchIndexName = INDEX_NAME,
            searchText = "kotlin",
            searchableFields = listOf("title", "content"),
            paginationRequest = PaginationRequest(
                limit = 2,
                pageToken = firstPage.nextPageToken!!
            )
        )

        assertThat(secondPage.items).hasSize(2)
        assertThat(secondPage.nextPageToken).isNotNull

        // Verify no overlap between pages
        val firstPageIds = firstPage.items.map { it.id }
        val secondPageIds = secondPage.items.map { it.id }
        assertThat(firstPageIds).doesNotContainAnyElementsOf(secondPageIds)
    }
}
