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
        private val fieldAliases = mapOf(
            "title" to "document.title",
            "content" to "document.content",
            "createdAt" to "document.createdAt",
            "updatedAt" to "document.updatedAt"
        )

        override fun search(
            searchText: String,
            paginationRequest: PaginationRequest,
            configOverride: SearchConfiguration?
        ): PaginationSlice<TestEntity> {
            val paginationRequestAliased = SearchIndex.Helper.createAliasedPaginationRequest(
                paginationRequest = paginationRequest,
                fieldAliases = fieldAliases
            )

            return atlasSearchQuery.search(
                entityClass = TestEntity::class,
                searchIndexName = getIndexName(),
                paginationRequest = paginationRequestAliased,
                searchText = searchText,
                configuration = configOverride ?: getSearchConfiguration()
            )
        }

        override fun getSearchConfiguration() = SearchConfiguration(
            textSearchFields = listOf("document.title", "document.content"),
            scoreFields = listOf(
                ScoreField(
                    field = "document.title",
                    score = Score.Boost(2.0)
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
        mongoTemplate.dropCollection(testSearchIndex.getTargetCollectionName())

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
                lastUpdated = now
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
                lastUpdated = now.minusSeconds(3600)
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

    @Test
    fun `should create search index with correct scoring configuration`() {
        // Initialize search indexes
        atlasSearchConfig.initializeSearchIndexes()

        // Verify index was created with correct configuration
        val listIndexesCommand = Document("listSearchIndexes", testSearchIndex.getTargetCollectionName())
        val indexes = mongoTemplate.db.runCommand(listIndexesCommand)
        val cursor = indexes.get("cursor") as? Document
        val firstBatch = cursor?.get("firstBatch") as? List<*>
        val indexDefinition = (firstBatch?.first() as? Document)?.get("latestDefinition") as? Document

        assertThat(indexDefinition).isNotNull
        val mappings = indexDefinition?.get("mappings") as? Document

        // Verify field mappings
        val fields = mappings?.get("fields") as? Document
        assertThat(fields).isNotNull

        // Verify document field structure
        val documentField = fields?.get("document") as? Document
        assertThat(documentField?.get("type")).isEqualTo("document")

        val documentFields = documentField?.get("fields") as? Document
        assertThat(documentFields?.get("title")).isNotNull
        assertThat(documentFields?.get("content")).isNotNull
        assertThat(documentFields?.get("createdAt")).isNotNull
        assertThat(documentFields?.get("updatedAt")).isNotNull

        // Verify numeric and date fields
        assertThat(fields?.get("score")).isNotNull
        assertThat(fields?.get("viewCount")).isNotNull
        assertThat(fields?.get("lastUpdated")).isNotNull
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

        // First document should have higher score due to:
        // 1. Title boost (contains "Test")
        // 2. More recent creation date
        // 3. Higher score value
        assertThat(result.items.first().id).isEqualTo("1")
    }
}
