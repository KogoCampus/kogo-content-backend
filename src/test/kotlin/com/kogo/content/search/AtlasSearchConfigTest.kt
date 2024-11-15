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
        val title: String,
        val content: String,
        val score: Double,
        val createdAt: Instant
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
            boost: Double?
        ): PaginationSlice<TestEntity> {
            return atlasSearchQuery.search(
                entityClass = TestEntity::class,
                searchIndex = getIndexName(),
                paginationRequest = paginationRequest,
                searchText = searchText,
                searchFields = getSearchFields(),
                scoreFields = listOf(
                    AtlasSearchQueryBuilder.ScoreField(
                        field = "score",
                        boost = boost ?: 1.0
                    )
                )
            )
        }

        override fun getSearchFields(): List<String> = listOf("title", "content")
        override fun getIndexName(): String = "atlas_config_test_index"
        override fun getCollectionName(): String = "atlas_config_test_entities"
        override fun getMapping(): SearchMapping = SearchMapping.builder()
            .dynamic(false)
            .addField("title", FieldType.STRING, "lucene.standard")
            .addField("content", FieldType.STRING, "lucene.standard")
            .addField("score", FieldType.NUMBER)
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
                title = "Test Document 1",
                content = "This is a test document with some content",
                score = 0.8,
                createdAt = Instant.now()
            ),
            TestEntity(
                id = "2",
                title = "Another Test",
                content = "More test content here",
                score = 0.5,
                createdAt = Instant.now().minusSeconds(60)
            )
        )
        mongoTemplate.insertAll(testData)
    }

    @Test
    fun `should create search index if it does not exist`() {
        // Initialize search indexes
        atlasSearchConfig.initializeSearchIndexes()

        // Verify index was created
        val listIndexesCommand = Document("listSearchIndexes", testSearchIndex.getCollectionName())
        val indexes = mongoTemplate.db.runCommand(listIndexesCommand)
        val cursor = indexes.get("cursor") as? Document
        val firstBatch = cursor?.get("firstBatch") as? List<*>

        assertThat(firstBatch).isNotEmpty
        assertThat(firstBatch?.any { (it as? Document)?.get("name") == testSearchIndex.getIndexName() }).isTrue
    }

    @Test
    fun `should not create search index if it already exists`() {
        // Create index first
        val createCommand = Document().apply {
            put("createSearchIndexes", testSearchIndex.getCollectionName())
            put("indexes", listOf(Document().apply {
                put("name", testSearchIndex.getIndexName())
                put("definition", testSearchIndex.getMapping().toDocument())
            }))
        }
        mongoTemplate.db.runCommand(createCommand)

        // Try to initialize indexes again
        atlasSearchConfig.initializeSearchIndexes()

        // Verify only one index exists
        val listIndexesCommand = Document("listSearchIndexes", testSearchIndex.getCollectionName())
        val indexes = mongoTemplate.db.runCommand(listIndexesCommand)
        val cursor = indexes.get("cursor") as? Document
        val firstBatch = cursor?.get("firstBatch") as? List<*>

        assertThat(firstBatch).isNotEmpty
        assertThat(firstBatch?.count { (it as? Document)?.get("name") == testSearchIndex.getIndexName() }).isEqualTo(1)
    }
}
