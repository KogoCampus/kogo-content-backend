package com.kogo.content.search

import com.kogo.content.lib.PaginationRequest
import com.kogo.content.lib.PaginationSlice
import org.assertj.core.api.Assertions.assertThat
import org.bson.Document
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

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
        val lastUpdated: Instant
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
        fun testSearchIndex(atlasSearchQuery: AtlasSearchQueryBuilder): TestSearchIndex {
            return TestSearchIndex(atlasSearchQuery)
        }
    }

    class TestSearchIndex(
        private val atlasSearchQuery: AtlasSearchQueryBuilder
    ) : SearchIndex<TestEntity> {
        override fun search(
            searchText: String,
            paginationRequest: PaginationRequest,
        ): PaginationSlice<TestEntity> {
            return atlasSearchQuery.search(
                entityClass = TestEntity::class,
                searchIndexName = getIndexName(),
                paginationRequest = paginationRequest,
                searchText = searchText,
                searchableFields = getSearchableFields(),
            )
        }

        override fun getSearchableFields(): List<String> = listOf(
            "document.title",
            "document.content"
        )

        override fun getIndexName(): String = "atlas_config_test_index"

        override fun getTargetCollectionName(): String = "atlas_config_test_entities"

        override fun getSearchIndexDefinition(): SearchIndexDefinition = SearchIndexDefinition.builder()
            .dynamic(false)
            .documentField("document") {
                stringField("title")
                stringField("content")
                dateField("createdAt")
                dateField("updatedAt")
            }
            .numberField("score")
            .numberField("viewCount")
            .dateField("lastUpdated")
            .build()
    }

    @BeforeEach
    fun setup() {
        // Clean up existing collection and indexes
        mongoTemplate.dropCollection(TestEntity::class.java)

        // Create test data
        val testData = listOf(
            TestEntity(
                id = "1",
                document = TestDocument(
                    title = "Test Document 1",
                    content = "This is a test document with some content",
                    createdAt = Instant.now(),
                    updatedAt = Instant.now()
                ),
                score = 0.8,
                viewCount = 100,
                lastUpdated = Instant.now()
            ),
            TestEntity(
                id = "2",
                document = TestDocument(
                    title = "Another Test",
                    content = "More test content here",
                    createdAt = Instant.now().minusSeconds(60),
                    updatedAt = Instant.now().minusSeconds(30)
                ),
                score = 0.5,
                viewCount = 50,
                lastUpdated = Instant.now().minusSeconds(60)
            )
        )
        mongoTemplate.insertAll(testData)
    }

    @Test
    fun `should create search index if it does not exist`() {
        // Initialize search indexes
        atlasSearchConfig.initializeSearchIndexes()

        // Verify index was created
        val listIndexesCommand = Document("listSearchIndexes", testSearchIndex.getTargetCollectionName())
        val indexes = mongoTemplate.db.runCommand(listIndexesCommand)
        val cursor = indexes.get("cursor") as? Document
        val firstBatch = cursor?.get("firstBatch") as? List<*>

        assertThat(firstBatch).isNotEmpty
        assertThat(firstBatch?.any { (it as? Document)?.get("name") == testSearchIndex.getIndexName() }).isTrue
    }

    @Test
    fun `should create index with correct mapping`() {
        // Initialize search indexes
        atlasSearchConfig.initializeSearchIndexes()

        // Verify index mapping
        val listIndexesCommand = Document("listSearchIndexes", testSearchIndex.getTargetCollectionName())
        val indexes = mongoTemplate.db.runCommand(listIndexesCommand)
        val cursor = indexes.get("cursor") as? Document
        val firstBatch = cursor?.get("firstBatch") as? List<*>

        val indexDefinition = (firstBatch?.first() as? Document)?.get("latestDefinition") as? Document

        assertThat(indexDefinition).isNotNull
        val mappings = indexDefinition?.get("mappings") as? Document
        assertThat(mappings?.get("dynamic")).isEqualTo(false)

        val fields = mappings?.get("fields") as? Document
        assertThat(fields).isNotNull
        assertThat(fields?.containsKey("document")).isTrue
        assertThat(fields?.containsKey("score")).isTrue
        assertThat(fields?.containsKey("viewCount")).isTrue
        assertThat(fields?.containsKey("lastUpdated")).isTrue
    }
}
