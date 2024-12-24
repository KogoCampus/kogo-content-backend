package com.kogo.content.search

import com.kogo.content.endpoint.common.PaginationRequest
import com.kogo.content.endpoint.common.PaginationSlice
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Instant
import java.util.Date

@SpringBootTest
@ActiveProfiles("test")
class AtlasSearchConfigTest {

    @Autowired
    private lateinit var mongoTemplate: MongoTemplate

    @Autowired
    private lateinit var atlasSearchConfig: AtlasSearchConfig

    @Autowired
    private lateinit var testSearchIndex: TestSearchIndex

    @org.springframework.data.mongodb.core.mapping.Document("atlas_config_test_entities")
    data class TestEntity(
        @Id
        val id: String,
        val document: TestDocument,
        val score: Double,
        val viewCount: Int,
        val lastUpdated: Instant,
        val tags: List<String> = emptyList(),
        val metadata: Map<String, Any> = emptyMap()
    )

    data class TestDocument(
        val title: String,
        val content: String,
        val createdAt: Instant,
        val updatedAt: Instant
    )

    @TestConfiguration
    class TestConfig {
        @Bean
        fun testSearchIndex(): TestSearchIndex {
            return TestSearchIndex()
        }
    }

    class TestSearchIndex : SearchIndex<TestEntity>(TestEntity::class) {

        override fun defaultSearchConfiguration() = SearchConfiguration(
            textSearchFields = listOf("document.title", "document.content", "tags"),
            scoreFields = listOf(
                ScoreField(
                    field = "document.title",
                    score = Score.Boost(2.0)
                ),
                ScoreField(
                    field = "tags",
                    score = Score.Boost(1.5)
                )
            ),
            nearFields = listOf(
                DateNearField(
                    field = "document.createdAt",
                    origin = Date(),
                    pivot = DateNearField.ONE_DAY_MS,
                    score = Score.Boost(1.5)
                ),
                NumericNearField(
                    field = "score",
                    origin = 1.0,
                    pivot = 500,
                    score = Score.Boost(1.2)
                )
            )
        )

        override fun indexName(): String = "atlas_config_test_index"

        override fun mongoEntityCollectionName(): String = "atlas_config_test_entities"

        override fun searchIndexDefinition(): SearchIndexDefinition = SearchIndexDefinition.builder()
            .dynamic(true)
            .documentField("document") {
                stringField("title")
                stringField("content")
                dateField("createdAt")
                dateField("updatedAt")
            }
            .numberField("score")
            .numberField("viewCount")
            .dateField("lastUpdated")
            .stringField("tags")
            .build()
    }

    @BeforeEach
    fun setup() {
        mongoTemplate.dropCollection(testSearchIndex.mongoEntityCollectionName())
        mongoTemplate.dropCollection("search_index_versions")

        val now = Instant.now()
        val testData = listOf(
            TestEntity(
                id = "1",
                document = TestDocument(
                    title = "Test Document Title",
                    content = "This is a test document with important test content",
                    createdAt = now,
                    updatedAt = now
                ),
                score = 1.0,
                viewCount = 100,
                lastUpdated = now,
                tags = listOf("test", "important"),
                metadata = mapOf("category" to "test")
            ),
            TestEntity(
                id = "2",
                document = TestDocument(
                    title = "Another Document",
                    content = "More test content here",
                    createdAt = now.minusSeconds(3600),
                    updatedAt = now.minusSeconds(30)
                ),
                score = 0.5,
                viewCount = 50,
                lastUpdated = now.minusSeconds(3600),
                tags = listOf("other", "test"),
                metadata = mapOf("category" to "other")
            )
        )
        mongoTemplate.insertAll(testData)
    }

    @Test
    fun `should create search index if it does not exist`() {
        atlasSearchConfig.initializeSearchIndexes()

        val listIndexesCommand = Document("listSearchIndexes", testSearchIndex.mongoEntityCollectionName())
        val indexes = mongoTemplate.db.runCommand(listIndexesCommand)
        val cursor = indexes.get("cursor") as? Document
        val firstBatch = cursor?.get("firstBatch") as? List<*>

        assertThat(firstBatch).isNotEmpty
        assertThat(firstBatch?.any { (it as? Document)?.get("name") == testSearchIndex.indexName() }).isTrue
    }

    @Test
    fun `should create index with correct mapping`() {
        atlasSearchConfig.initializeSearchIndexes()

        val listIndexesCommand = Document("listSearchIndexes", testSearchIndex.mongoEntityCollectionName())
        val indexes = mongoTemplate.db.runCommand(listIndexesCommand)
        val cursor = indexes.get("cursor") as? Document
        val firstBatch = cursor?.get("firstBatch") as? List<*>
        val indexDefinition = (firstBatch?.first() as? Document)?.get("latestDefinition") as? Document

        assertThat(indexDefinition).isNotNull
        val mappings = indexDefinition?.get("mappings") as? Document
        assertThat(mappings?.get("dynamic")).isEqualTo(true)

        val fields = mappings?.get("fields") as? Document
        assertThat(fields).isNotNull
        assertThat(fields?.containsKey("document")).isTrue
        assertThat(fields?.containsKey("score")).isTrue
        assertThat(fields?.containsKey("viewCount")).isTrue
        assertThat(fields?.containsKey("lastUpdated")).isTrue
        assertThat(fields?.containsKey("tags")).isTrue
    }

    @Test
    fun `should store and track index version`() {
        atlasSearchConfig.initializeSearchIndexes()

        val versionDoc = mongoTemplate.db.getCollection("search_index_versions")
            .find(Document("indexName", testSearchIndex.indexName()))
            .first()

        assertThat(versionDoc).isNotNull
        assertThat(versionDoc?.getString("version")).isNotNull
        assertThat(versionDoc?.get("updatedAt")).isNotNull
    }

    @Test
    fun `should perform search with configured scoring`() {
        atlasSearchConfig.initializeSearchIndexes()
        Thread.sleep(1000) // Wait for index to be ready

        val result = testSearchIndex.search(
            searchText = "test",
            paginationRequest = PaginationRequest(limit = 10)
        )

        assertThat(result.items).isNotEmpty
        assertThat(result.items).hasSize(2)

        // First document should have higher score due to:
        // 1. Title boost (contains "Test")
        // 2. More recent creation date
        // 3. Higher score value
        // 4. Tag boost
        assertThat(result.items.first().id).isEqualTo("1")
    }

    @Test
    fun `should handle dynamic fields in search`() {
        atlasSearchConfig.initializeSearchIndexes()
        Thread.sleep(1000)

        val result = testSearchIndex.search(
            searchText = "test",
            paginationRequest = PaginationRequest(limit = 10)
        )

        assertThat(result.items).isNotEmpty
        assertThat(result.items.first().metadata).isNotEmpty
        assertThat(result.items.first().metadata["category"]).isEqualTo("test")
    }

    @Test
    fun `should perform tag-based search`() {
        atlasSearchConfig.initializeSearchIndexes()
        Thread.sleep(1000)

        val result = testSearchIndex.search(
            searchText = "important",
            paginationRequest = PaginationRequest(limit = 10)
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().tags).contains("important")
    }
}