package com.kogo.content.storage.pagination

import com.kogo.content.exception.InvalidFieldException
import com.kogo.content.common.*
import com.kogo.content.endpoint.common.*
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
        val author: User? = null
    )

    private val fieldMappings = mapOf(
        "id" to "_id",
        "name" to "name",
        "score" to "score",
        "createdAt" to "created_at",
        "author" to "author._id"
    )

    @BeforeEach
    fun setup() {
        mongoTemplate.dropCollection(TestEntity::class.java)

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
            request,
            fieldMappings = fieldMappings,
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
            firstPageRequest,
            fieldMappings = fieldMappings,
        )

        // Second page using cursor
        val secondPageRequest = PaginationRequest(
            limit = 2,
            pageToken = firstPage.nextPageToken!!
        )

        val secondPage = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            secondPageRequest,
            fieldMappings = fieldMappings,
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
            request,
            fieldMappings = fieldMappings,
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
            request,
            fieldMappings = fieldMappings,
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
            request,
            fieldMappings = fieldMappings,
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
            request,
            fieldMappings = fieldMappings,
        )

        assertThat(result.items).isEmpty()
        assertThat(result.nextPageToken).isNull()
    }

    @Test
    fun `should handle document reference fields`() {
        val author = User(id = "test-author", username = "testuser")
        mongoTemplate.save(author)

        val entityWithRef = TestEntity(
            id = "6",
            name = "Foxtrot",
            score = 400,
            createdAt = Instant.now(),
            author = author
        )
        mongoTemplate.save(entityWithRef)

        val request = PaginationRequest(limit = 1)
            .withFilter("author", author.id!!, FilterOperator.EQUALS)

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request,
            fieldMappings = fieldMappings,
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
                request,
                fieldMappings = fieldMappings,
            )
        }
    }

    @Test
    fun `should throw exception for excluded fields`() {
        val request = PaginationRequest(limit = 3)
            .withFilter("score", 200, FilterOperator.EQUALS)
            .withSort("name", SortDirection.ASC)

        val excludedFields = setOf("score")

        assertThrows<InvalidFieldException> {
            mongoPaginationQueryBuilder.getPage(
                TestEntity::class,
                request,
                fieldMappings = fieldMappings,
                excludedFields = excludedFields
            )
        }
    }

    @Test
    fun `should throw exception for unmapped fields`() {
        val incompleteFieldMappings = mapOf(
            "name" to "name"  // Only include name field
        )

        val request = PaginationRequest(limit = 2)
            .withFilter("nonexistentField", 200, FilterOperator.EQUALS)
            .withSort("name", SortDirection.ASC)

        assertThrows<InvalidFieldException> {
            mongoPaginationQueryBuilder.getPage(
                TestEntity::class,
                request,
                fieldMappings = incompleteFieldMappings,
            )
        }
    }

    @Test
    fun `should navigate through pages using encoded page tokens`() {
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
            firstPageRequest,
            fieldMappings = fieldMappings,
        )

        // Verify first page
        assertThat(firstPage.items).hasSize(3)
        assertThat(firstPage.items.map { it.score }).containsExactly(1000, 900, 800)
        assertThat(firstPage.nextPageToken).isNotNull

        // Navigate to second page using encoded token
        val encodedToken = firstPage.nextPageToken!!.encode()
        val decodedToken = PageToken.fromString(encodedToken)
        val secondPageRequest = PaginationRequest(
            limit = 3,
            pageToken = decodedToken
        )

        val secondPage = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            secondPageRequest,
            fieldMappings = fieldMappings,
        )

        // Verify second page
        assertThat(secondPage.items).hasSize(3)
        assertThat(secondPage.items.map { it.score }).containsExactly(700, 600, 500)
        assertThat(secondPage.nextPageToken).isNotNull

        // Navigate to third page
        val thirdPageRequest = PaginationRequest(
            limit = 3,
            pageToken = PageToken.fromString(secondPage.nextPageToken!!.encode())
        )

        val thirdPage = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            thirdPageRequest,
            fieldMappings = fieldMappings,
        )

        // Verify third page
        assertThat(thirdPage.items).hasSize(3)
        assertThat(thirdPage.items.map { it.score }).containsExactly(400, 300, 200)
        assertThat(thirdPage.nextPageToken).isNotNull

        // Navigate to final page
        val finalPageRequest = PaginationRequest(
            limit = 3,
            pageToken = PageToken.fromString(thirdPage.nextPageToken!!.encode())
        )

        val finalPage = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            finalPageRequest,
            fieldMappings = fieldMappings,
        )

        // Verify final page
        assertThat(finalPage.items).hasSize(1)  // Only one item remaining
        assertThat(finalPage.items.map { it.score }).containsExactly(100)
        assertThat(finalPage.nextPageToken).isNull()
    }

    @Test
    fun `should handle complex pagination with multiple sorts and filters`() {
        val request = PaginationRequest(
            limit = 2,
            pageToken = PageToken.create()
                .copy(
                    sortFields = listOf(
                        SortField("score", SortDirection.DESC),
                        SortField("createdAt", SortDirection.ASC)
                    ),
                    filterFields = listOf(
                        FilterField("score", listOf(200, 250, 300), FilterOperator.IN)
                    )
                )
        )

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request,
            fieldMappings = fieldMappings,
        )

        assertThat(result.items).hasSize(2)
        assertThat(result.items.map { it.score }).containsExactly(300, 250)
        assertThat(result.nextPageToken).isNotNull

        // Verify next page using encoded token
        val nextRequest = PaginationRequest(
            limit = 2,
            pageToken = PageToken.fromString(result.nextPageToken!!.encode())
        )

        val nextResult = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            nextRequest,
            fieldMappings = fieldMappings,
        )

        assertThat(nextResult.items).hasSize(1)
        assertThat(nextResult.items.map { it.score }).containsExactly(200)
        assertThat(nextResult.nextPageToken).isNull()
    }

    @Test
    fun `should handle date-based cursor pagination`() {
        val request = PaginationRequest(
            limit = 2,
            pageToken = PageToken.create()
                .copy(sortFields = listOf(SortField("createdAt", SortDirection.DESC)))
        )

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request,
            fieldMappings = fieldMappings,
        )

        assertThat(result.items).hasSize(2)
        assertThat(result.nextPageToken).isNotNull
        assertThat(result.nextPageToken?.cursors?.get("createdAt")?.type)
            .isEqualTo(CursorValueType.DATE)
    }

    @Test
    fun `should handle numeric comparison operators`() {
        val request = PaginationRequest(limit = 10)
            .withFilter("score", 200, FilterOperator.GREATER_THAN)
            .withFilter("score", 300, FilterOperator.LESS_THAN)
            .withSort("score", SortDirection.ASC)

        val result = mongoPaginationQueryBuilder.getPage(
            TestEntity::class,
            request,
            fieldMappings = fieldMappings,
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
            request,
            fieldMappings = fieldMappings,
        )

        // Should return entities created between 3 seconds ago and 1 second ago
        assertThat(result.items).isNotEmpty
        assertThat(result.items.all {
            it.createdAt.isAfter(threeSecondsAgo) && it.createdAt.isBefore(oneSecondAgo)
        }).isTrue()
    }
}
