package com.kogo.content.storage.pagination

import com.kogo.content.exception.InvalidFieldException
import com.kogo.content.endpoint.common.*
import com.kogo.content.storage.model.entity.SchoolInfo
import com.kogo.content.storage.model.entity.User
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.mapping.Document
import org.springframework.data.mongodb.core.mapping.DocumentReference
import org.springframework.data.mongodb.core.mapping.Field
import org.springframework.test.context.ActiveProfiles
import java.time.Instant

@SpringBootTest
@ActiveProfiles("test")
class MongoPaginationQueryBuilderTest @Autowired constructor(
    private val mongoTemplate: MongoTemplate,
    private val mongoPaginationQueryBuilder: MongoPaginationQueryBuilder
) {
    @Document("test_entities")
    data class TestEntity(
        @Id
        val id: String,
        val name: String,
        val score: Int,
        @Field("created_at")
        val createdAt: Instant,
        @DocumentReference
        val author: User? = null,
        val dynamicField: String? = null
    )

    private lateinit var testUser: User

    @BeforeEach
    fun setup() {
        mongoTemplate.dropCollection(TestEntity::class.java)
        mongoTemplate.dropCollection(User::class.java)

        // Create test user
        testUser = User(
            id = "test-author",
            username = "testuser",
            email = "test@example.com",
            schoolInfo = SchoolInfo(
                schoolKey = "TEST",
                schoolName = "Test School",
                schoolShortenedName = "TS"
            ),
            followingGroupIds = mutableListOf()
        )
        mongoTemplate.save(testUser)

        // Create test data
        val testData = listOf(
            TestEntity("1", "Alpha", 100, Instant.now().minusSeconds(5)),
            TestEntity("2", "Beta", 200, Instant.now().minusSeconds(4)),
            TestEntity("3", "Charlie", 150, Instant.now().minusSeconds(3)),
            TestEntity("4", "Delta", 300, Instant.now().minusSeconds(2)),
            TestEntity("5", "Echo", 250, Instant.now().minusSeconds(1))
        )
        mongoTemplate.insertAll(testData)
    }

    @Test
    fun `should return first page of results with default sorting`() {
        val request = PaginationRequest(
            limit = 2,
            pageToken = PageToken.create().copy(
                sortFields = listOf(
                    SortField("score", SortDirection.DESC)
                )
            )
        )

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request
        )

        assertThat(result.items).hasSize(2)
        assertThat(result.items.map { it.score }).containsExactly(300, 250)
        assertThat(result.nextPageToken).isNotNull
        assertThat(result.nextPageToken?.cursors?.get("score")?.value).isEqualTo(250)
    }

    @Test
    fun `should return next page using cursor`() {
        // First page
        val firstPageRequest = PaginationRequest(
            limit = 2,
            pageToken = PageToken.create().copy(
                sortFields = listOf(
                    SortField("score", SortDirection.DESC)
                )
            )
        )

        val firstPage = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            firstPageRequest
        )

        // Second page using cursor
        val secondPageRequest = PaginationRequest(
            limit = 2,
            pageToken = firstPage.nextPageToken!!
        )

        val secondPage = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            secondPageRequest
        )

        assertThat(secondPage.items).hasSize(2)
        assertThat(secondPage.items.map { it.score }).containsExactly(200, 150)
        assertThat(secondPage.nextPageToken).isNotNull
    }

    @Test
    fun `should apply multiple sorts`() {
        val request = PaginationRequest(
            limit = 3,
            pageToken = PageToken.create().copy(
                sortFields = listOf(
                    SortField("score", SortDirection.DESC),
                    SortField("name", SortDirection.ASC)
                )
            )
        )

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request
        )

        assertThat(result.items).hasSize(3)
        assertThat(result.items.map { it.score to it.name })
            .containsExactly(
                300 to "Delta",
                250 to "Echo",
                200 to "Beta"
            )
    }

    @Test
    fun `should apply filters`() {
        val request = PaginationRequest(limit = 10)
            .withFilter("score", 200, FilterOperator.EQUALS)
            .withSort("name", SortDirection.ASC)

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().name).isEqualTo("Beta")
    }

    @Test
    fun `should handle IN operator filter`() {
        val request = PaginationRequest(limit = 10)
            .withFilter("score", listOf(200, 300), FilterOperator.IN)
            .withSort("score", SortDirection.ASC)

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request
        )

        assertThat(result.items).hasSize(2)
        assertThat(result.items.map { it.score }).containsExactlyInAnyOrder(200, 300)
    }

    @Test
    fun `should handle empty results`() {
        val request = PaginationRequest(limit = 10)
            .withFilter("score", 999, FilterOperator.EQUALS)

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request
        )

        assertThat(result.items).isEmpty()
        assertThat(result.nextPageToken).isNull()
    }

    @Test
    fun `should handle document reference fields`() {
        val entityWithRef = TestEntity(
            id = "6",
            name = "Foxtrot",
            score = 400,
            createdAt = Instant.now(),
            author = testUser
        )
        mongoTemplate.save(entityWithRef)

        val request = PaginationRequest(limit = 1)
            .withFilter("author", testUser.id!!, FilterOperator.EQUALS)

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items.first().author?.id).isEqualTo("test-author")
    }

    @Test
    fun `should throw exception for non-existent fields`() {
        val request = PaginationRequest(limit = 3)
            .withFilter("nonExistentField", "value", FilterOperator.EQUALS)
            .withSort("name", SortDirection.ASC)

        assertThrows<InvalidFieldException> {
            mongoPaginationQueryBuilder.getPage(
                TestEntity::class,
                request
            )
        }
    }

    @Test
    fun `should allow dynamic fields when specified`() {
        val request = PaginationRequest(limit = 3)
            .withFilter("dynamicScore", 100, FilterOperator.EQUALS)
            .withSort("dynamicScore", SortDirection.DESC)

        val allowedDynamicFields = setOf("dynamicScore")

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request,
            allowedDynamicFields = allowedDynamicFields
        )

        assertThat(result.items).isEmpty()
    }

    @Test
    fun `should handle numeric comparison operators`() {
        val request = PaginationRequest(limit = 10)
            .withFilter("score", 200, FilterOperator.GREATER_THAN)
            .withFilter("score", 300, FilterOperator.LESS_THAN)
            .withSort("score", SortDirection.ASC)

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request
        )

        assertThat(result.items).hasSize(1)
        assertThat(result.items.map { it.score }).containsExactly(250)
        assertThat(result.items.all { it.score in 201..299 }).isTrue()
    }

    @Test
    fun `should handle date comparison operators`() {
        val now = Instant.now()
        val threeSecondsAgo = now.minusSeconds(3)
        val oneSecondAgo = now.minusSeconds(1)

        val request = PaginationRequest(limit = 10)
            .withFilter("createdAt", threeSecondsAgo, FilterOperator.GREATER_THAN)
            .withFilter("createdAt", oneSecondAgo, FilterOperator.LESS_THAN)
            .withSort("createdAt", SortDirection.DESC)

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request
        )

        assertThat(result.items).isNotEmpty
        assertThat(result.items.all {
            it.createdAt.isAfter(threeSecondsAgo) && it.createdAt.isBefore(oneSecondAgo)
        }).isTrue()
    }

    @Test
    fun `should handle pre-aggregation operations`() {
        val request = PaginationRequest(limit = 2)
            .withSort("score", SortDirection.DESC)

        val preAggregationOperations = listOf(
            org.springframework.data.mongodb.core.aggregation.Aggregation.match(
                org.springframework.data.mongodb.core.query.Criteria.where("score").gt(200)
            )
        )

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request,
            preAggregationOperations = preAggregationOperations
        )

        assertThat(result.items).hasSize(2)
        assertThat(result.items.map { it.score }).containsExactly(300, 250)
        assertThat(result.items.all { it.score > 200 }).isTrue()
    }

    @Test
    fun `should navigate through all pages with encoded tokens`() {
        // Clear existing data first
        mongoTemplate.dropCollection(TestEntity::class.java)

        // Create test data with unique IDs
        val testData = (1..10).map { i ->
            TestEntity(
                id = (i + 5).toString(),
                name = "Entity$i",
                score = (11 - i) * 100,
                createdAt = Instant.now().minusSeconds(i.toLong())
            )
        }
        mongoTemplate.insertAll(testData)

        // First page
        val firstPageRequest = PaginationRequest(
            limit = 3,
            pageToken = PageToken.create()
                .copy(sortFields = listOf(SortField("score", SortDirection.DESC)))
        )

        val firstPage = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            firstPageRequest
        )

        // Verify first page
        assertThat(firstPage.items).hasSize(3)
        assertThat(firstPage.items.map { it.score }).containsExactly(1000, 900, 800)
        assertThat(firstPage.nextPageToken).isNotNull

        // Navigate through all pages using encoded tokens
        var currentPage = firstPage
        var pageCount = 1
        val allScores = mutableListOf<Int>()
        allScores.addAll(currentPage.items.map { it.score })

        while (currentPage.nextPageToken != null) {
            val nextRequest = PaginationRequest(
                limit = 3,
                pageToken = PageToken.fromString(currentPage.nextPageToken!!.encode())
            )

            currentPage = mongoPaginationQueryBuilder.getPage(
                TestEntity::class,
                nextRequest
            )

            allScores.addAll(currentPage.items.map { it.score })
            pageCount++
        }

        assertThat(pageCount).isEqualTo(4)
        assertThat(allScores).containsExactly(1000, 900, 800, 700, 600, 500, 400, 300, 200, 100)
    }
}

